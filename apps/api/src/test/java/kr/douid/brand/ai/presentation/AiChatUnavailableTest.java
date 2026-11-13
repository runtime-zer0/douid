package kr.douid.brand.ai.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import kr.douid.brand.ai.infrastructure.config.AiChatConfig;
import kr.douid.brand.auth.application.AuthenticationService;
import kr.douid.brand.shared.config.SecurityConfig;

@WebMvcTest(value=AiChatController.class,properties={"app.ai-chat.rate-limit.requests=2"})
@Import({AiChatConfig.class,SecurityConfig.class})
class AiChatUnavailableTest {
    @Autowired MockMvc mvc;
    @MockitoBean AuthenticationService auth;
    @Test void 매핑전_406_415는_집계제외하며_503도_요청한도_소비() throws Exception {
        mvc.perform(post("/api/public/ai-chat/stream").contentType(MediaType.TEXT_PLAIN).content("secret"))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.data.code").value("UNSUPPORTED_MEDIA_TYPE"));
        mvc.perform(post("/api/public/ai-chat/stream").contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_XML).content("{}"))
                .andExpect(status().isNotAcceptable()).andExpect(jsonPath("$.data.code").value("NOT_ACCEPTABLE"));
        for(int i=0;i<2;i++)mvc.perform(post("/api/public/ai-chat/stream").contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM).content("{\"message\":\"hi\"}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.data.code").value("AI_UNAVAILABLE"))
                .andExpect(request().asyncNotStarted()).andExpect(cookie().doesNotExist("JSESSIONID"));
        mvc.perform(post("/api/public/ai-chat/stream").contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"hi\"}"))
                .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.data.code").value("AI_RATE_LIMITED"));
    }
}
