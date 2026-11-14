package kr.douid.brand.ai.presentation;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import kr.douid.brand.ai.application.*;
import kr.douid.brand.ai.infrastructure.guard.InMemoryChatRequestGuard;
import kr.douid.brand.ai.infrastructure.stream.BoundedChatStreamExecutor;
import kr.douid.brand.ai.presentation.stream.AiChatAsyncInterceptor;
import kr.douid.brand.ai.testsupport.*;
import kr.douid.brand.shared.presentation.GlobalExceptionHandler;

@ExtendWith(OutputCaptureExtension.class)
class AiChatPrivacyTest {
    @Test void 실제adapter의_Count와_생성_오류도_원문을_로그에_남기지않음(CapturedOutput output) throws Exception {
        try (var stub=new OpenAiStubServer();var client=java.net.http.HttpClient.newHttpClient();
             var executor=new BoundedChatStreamExecutor(2)) {
            var properties=new kr.douid.brand.ai.infrastructure.config.OpenAiProperties();
            properties.setEnabled(true);properties.setApiKey("KEY_CANARY");
            var gateway=kr.douid.brand.ai.infrastructure.openai.OpenAiResponsesGateway.forLoopbackTest(properties,client,executor,
                    new io.micrometer.core.instrument.simple.SimpleMeterRegistry(),stub.endpoint());
            var policy=Policies.defaults();
            var mvc=MockMvcBuilders.standaloneSetup(new AiChatController(new AiChatUseCase(gateway,
                    new InMemoryChatRequestGuard(policy,System::nanoTime),policy,executor),policy))
                    .addInterceptors(new AiChatAsyncInterceptor()).setControllerAdvice(new GlobalExceptionHandler()).build();
            for(boolean fail:new boolean[]{false,true}) {
                stub.generation=e->OpenAiStubServer.reply(e,fail?500:200,fail?"application/json":"text/event-stream",
                        fail?"PROVIDER_ERROR_CANARY":OpenAiStubServer.success("ANSWER_CANARY"));
                var result=mvc.perform(post("/api/public/ai-chat/stream").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"QUESTION_CANARY\",\"history\":[{\"role\":\"assistant\",\"content\":\"HISTORY_CANARY\"}]}"))
                        .andReturn();result.getAsyncResult(5000);
                String body=mvc.perform(asyncDispatch(result)).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
                assertThat(body).contains(fail?"STREAM_FAILED":"event:completed").doesNotContain("KEY_CANARY","PROVIDER_ERROR_CANARY");
            }
            assertThat(stub.countRequests).hasSize(2);assertThat(stub.generationRequests).hasSize(2);
            assertThat(output.getAll()).doesNotContain("QUESTION_CANARY","HISTORY_CANARY","ANSWER_CANARY","KEY_CANARY","PROVIDER_ERROR_CANARY");
        }
    }
    @Test void 질문_이력_답변_키_예외원문이_로그와_오류에_미노출(CapturedOutput output) throws Exception {
        var gateway=new ControlledLlmGateway();
        gateway.behavior=(s,d)->{d.accept("ANSWER_CANARY");throw new RuntimeException("KEY_EXCEPTION_CANARY");};
        var policy=Policies.defaults();
        try(var executor=new BoundedChatStreamExecutor(8)) {
            var mvc=MockMvcBuilders.standaloneSetup(new AiChatController(new AiChatUseCase(gateway,
                    new InMemoryChatRequestGuard(policy,System::nanoTime),policy,executor),policy))
                    .addInterceptors(new AiChatAsyncInterceptor()).setControllerAdvice(new GlobalExceptionHandler()).build();
            var result=mvc.perform(post("/api/public/ai-chat/stream").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"message\":\"QUESTION_CANARY\",\"history\":[{\"role\":\"assistant\",\"content\":\"HISTORY_CANARY\"}]}"))
                    .andReturn();result.getAsyncResult(3000);
            String body=mvc.perform(asyncDispatch(result)).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(body).contains("ANSWER_CANARY","STREAM_FAILED");
            String error=body.substring(body.indexOf("event:error"));
            assertThat(error).doesNotContain("QUESTION_CANARY","HISTORY_CANARY","ANSWER_CANARY","KEY_EXCEPTION_CANARY");
            String invalid=mvc.perform(post("/api/public/ai-chat/stream").contentType(MediaType.APPLICATION_JSON)
                    .content("{JSON_PARSE_CANARY")).andReturn().getResponse().getContentAsString();
            assertThat(invalid).doesNotContain("JSON_PARSE_CANARY");
            assertThat(output.getAll()).doesNotContain("QUESTION_CANARY","HISTORY_CANARY","ANSWER_CANARY",
                    "KEY_EXCEPTION_CANARY","JSON_PARSE_CANARY");
            assertThat(output.getAll()).contains("AI stream requestId=");
        }
    }
}
