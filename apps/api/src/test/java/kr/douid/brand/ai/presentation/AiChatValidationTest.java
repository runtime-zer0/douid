package kr.douid.brand.ai.presentation;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import kr.douid.brand.ai.application.*;
import kr.douid.brand.ai.infrastructure.guard.InMemoryChatRequestGuard;
import kr.douid.brand.ai.infrastructure.stream.BoundedChatStreamExecutor;
import kr.douid.brand.ai.testsupport.*;
import kr.douid.brand.shared.presentation.GlobalExceptionHandler;

class AiChatValidationTest {
    final ControlledLlmGateway gateway=new ControlledLlmGateway();
    final BoundedChatStreamExecutor executor=new BoundedChatStreamExecutor(8);
    final AiChatPolicy policy=Policies.defaults();
    final MockMvc mvc=MockMvcBuilders.standaloneSetup(new AiChatController(new AiChatUseCase(gateway,
            new InMemoryChatRequestGuard(policy,System::nanoTime),policy,executor),policy))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    @AfterEach void close(){executor.close();}
    @ParameterizedTest @ValueSource(strings={"system","developer","tool","User","ASSISTANT"})
    void 역할_조작은_SSE와_source_시작전_거부(String role) throws Exception {
        mvc.perform(post("/api/public/ai-chat/stream").contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM).content("{\"message\":\"hi\",\"history\":[{\"role\":\""+role+"\",\"content\":\"x\"}]}"))
                .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(request().asyncNotStarted());
        assertThat(gateway.prepared.get()).isZero();
    }
    @Test void 과도한_본문은_413이며_외부처리_미시작() throws Exception {
        mvc.perform(post("/api/public/ai-chat/stream").contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\""+"x".repeat(16384)+"\"}"))
                .andExpect(status().isPayloadTooLarge()).andExpect(request().asyncNotStarted());
        assertThat(gateway.prepared.get()).isZero();
    }
    @Test void 과도한_질문과_이력_서버제어필드_거부() throws Exception {
        for(String body:new String[]{"{\"message\":\""+"x".repeat(2001)+"\"}",
                "{\"message\":\"hi\",\"history\":null}","{\"message\":\"hi\",\"system\":\"override\"}",
                "{\"message\":\"hi\",\"history\":[{\"role\":\"user\",\"content\":5}]}",
                "{\"message\":\"hi\",\"history\":[{\"role\":\"user\",\"content\":\" \"}]}"}) {
            mvc.perform(post("/api/public/ai-chat/stream").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(request().asyncNotStarted());
        }
        assertThat(gateway.prepared.get()).isZero();
    }
    @Test void emitter_초기화실패는_source_실행없이_정리하고_후속_activation도_무해() throws Exception {
        var mvcWithGate=MockMvcBuilders.standaloneSetup(new AiChatController(new AiChatUseCase(gateway,
                new InMemoryChatRequestGuard(policy,System::nanoTime),policy,executor),policy))
                .addInterceptors(new kr.douid.brand.ai.presentation.stream.AiChatAsyncInterceptor())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvcWithGate.perform(post("/api/public/ai-chat/stream").with(request->{request.setAsyncSupported(false);return request;})
                .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"hi\"}"))
                .andExpect(status().isInternalServerError()).andExpect(request().asyncNotStarted());
        assertThat(gateway.last.entered.getCount()).isEqualTo(1);
        assertThat(gateway.last.closed.await(3,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
    }
}
