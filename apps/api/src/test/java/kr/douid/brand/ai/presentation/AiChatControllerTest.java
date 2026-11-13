package kr.douid.brand.ai.presentation;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import kr.douid.brand.ai.infrastructure.config.AiChatConfig;
import kr.douid.brand.ai.testsupport.*;
import kr.douid.brand.auth.application.AuthenticationService;
import kr.douid.brand.shared.config.SecurityConfig;

@WebMvcTest(AiChatController.class)
@Import({AiChatConfig.class,AiChatTestConfiguration.class,SecurityConfig.class,
        kr.douid.brand.ai.presentation.stream.AiChatWebConfig.class})
class AiChatControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ControlledLlmGateway gateway;
    @MockitoBean AuthenticationService auth;
    @BeforeEach void reset() {gateway.behavior=(stream,delta)->{};}
    MvcResult startRequest() throws Exception {
        return mvc.perform(post("/api/public/ai-chat/stream").contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM).content("{\"message\":\"안내\"}"))
                .andExpect(status().isOk()).andExpect(request().asyncStarted()).andReturn();
    }
    @Test void 익명_POST_다중_Delta와_JSON_escaping_정상종료() throws Exception {
        gateway.behavior=(stream,delta)->{delta.accept("한글😀\n\"따옴표");delta.accept("다음");};
        var result=startRequest();result.getAsyncResult(4000);
        var response=mvc.perform(asyncDispatch(result)).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(header().string("Cache-Control","no-store, no-transform"))
                .andExpect(header().string("X-Accel-Buffering","no")).andReturn().getResponse();
        String body=response.getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).containsSubsequence("event:started", "event:message_delta", "event:message_delta", "event:completed");
        assertThat(body).contains("\\n\\\"따옴표", "\"status\":\"success\"").doesNotContain("event:error");
        assertThat(result.getRequest().getSession(false)).isNull();
    }
    @Test void Delta_0개도_유일한_completed() throws Exception {
        var result=startRequest();result.getAsyncResult(4000);
        String body=mvc.perform(asyncDispatch(result)).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).contains("event:started","event:completed").doesNotContain("event:message_delta");
    }
    @Test void 오류_event는_정확한_필드와_자료형을_유지() throws Exception {
        gateway.behavior=(stream,delta)->{delta.accept("partial");throw new IllegalStateException("private");};
        var result=startRequest();result.getAsyncResult(3000);
        String body=mvc.perform(asyncDispatch(result)).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String frame=body.substring(body.indexOf("event:error"));
        String json=frame.substring(frame.indexOf("data:")+5).strip();
        var node=tools.jackson.databind.json.JsonMapper.builder().build().readTree(json);
        assertThat(node.size()).isEqualTo(3);
        assertThat(node.get("code").stringValue()).isEqualTo("STREAM_FAILED");
        assertThat(node.get("message").stringValue()).isEqualTo("응답을 처리하지 못했습니다.");
        assertThat(node.get("retryable").isBoolean()).isTrue();
        assertThat(node.get("retryable").booleanValue()).isFalse();
        assertThat(body).doesNotContain("event:completed","private");
    }
}
