package kr.douid.brand.auth.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import kr.douid.brand.auth.application.AdminResult;
import kr.douid.brand.auth.application.AuthenticationService;
import kr.douid.brand.auth.domain.AdminRole;
import kr.douid.brand.client.application.ClientAuthenticationService;
import kr.douid.brand.shared.config.SecurityConfig;
import kr.douid.brand.shared.presentation.GlobalExceptionHandler;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private ClientAuthenticationService clientAuthenticationService;

    @Test
    @WithMockUser(username = "admin@douid.kr")
    void me_인증됨_200_관리자정보() throws Exception {
        given(authenticationService.getCurrentAdmin())
                .willReturn(new AdminResult(1L, "admin@douid.kr", AdminRole.ADMIN));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("admin@douid.kr"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    @WithAnonymousUser
    void me_미인증_401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.code").value("UNAUTHORIZED"));
    }

    @Test
    @WithAnonymousUser
    void csrfToken_미인증_상태에서도_200() throws Exception {
        mockMvc.perform(get("/api/auth/csrf-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").exists());
    }
}
