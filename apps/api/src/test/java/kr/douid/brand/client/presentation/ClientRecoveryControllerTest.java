package kr.douid.brand.client.presentation;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import kr.douid.brand.auth.application.AuthenticationService;
import kr.douid.brand.client.application.ClientAuthenticationService;
import kr.douid.brand.client.application.RecoveryConfirmationService;
import kr.douid.brand.client.application.RecoveryRequestService;
import kr.douid.brand.client.domain.RateLimitExceededException;
import kr.douid.brand.client.domain.RecoveryTokenExpiredException;
import kr.douid.brand.client.domain.RecoveryTokenInvalidException;
import kr.douid.brand.shared.config.SecurityConfig;
import kr.douid.brand.shared.presentation.GlobalExceptionHandler;
import kr.douid.brand.shared.security.ClientTokenCookieProvider;

@WebMvcTest(ClientRecoveryController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@WithAnonymousUser
class ClientRecoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private ClientAuthenticationService clientAuthenticationService;

    @MockitoBean
    private RecoveryRequestService recoveryRequestService;

    @MockitoBean
    private RecoveryConfirmationService recoveryConfirmationService;

    @MockitoBean
    private ClientTokenCookieProvider clientTokenCookieProvider;

    @Test
    void request_등록여부무관하게_동일한202를반환한다() throws Exception {
        mockMvc.perform(post("/api/client/recovery/request").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.message").exists());
    }

    @Test
    void request_이메일형식오류_400() throws Exception {
        mockMvc.perform(post("/api/client/recovery/request").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalid\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void request_rateLimit초과_429() throws Exception {
        doThrow(new RateLimitExceededException())
                .when(recoveryRequestService).request(anyString());

        mockMvc.perform(post("/api/client/recovery/request").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.data.code").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void confirm_정상복원_200_새쿠키발급() throws Exception {
        given(recoveryConfirmationService.confirm(anyString())).willReturn("new-raw-token");
        given(clientTokenCookieProvider.issueCookie("new-raw-token"))
                .willReturn(ResponseCookie.from("client_token", "new-raw-token").httpOnly(true).build());

        mockMvc.perform(post("/api/client/recovery/confirm").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"raw-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recovered").value(true))
                .andExpect(header().string("Set-Cookie", containsString("client_token=new-raw-token")));
    }

    @Test
    void confirm_임시Identity쿠키가있어도_토큰이가리키는Identity로복원되고새쿠키로덮어쓴다() throws Exception {
        given(clientAuthenticationService.resolve("temporary-token")).willReturn(Optional.of(999L));
        given(recoveryConfirmationService.confirm(anyString())).willReturn("new-raw-token");
        given(clientTokenCookieProvider.issueCookie("new-raw-token"))
                .willReturn(ResponseCookie.from("client_token", "new-raw-token").httpOnly(true).build());

        mockMvc.perform(post("/api/client/recovery/confirm").with(csrf())
                        .cookie(new Cookie("client_token", "temporary-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"raw-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recovered").value(true))
                .andExpect(header().string("Set-Cookie", containsString("client_token=new-raw-token")));
    }

    @Test
    void confirm_토큰형식오류_400() throws Exception {
        mockMvc.perform(post("/api/client/recovery/confirm").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void confirm_만료된토큰_409() throws Exception {
        given(recoveryConfirmationService.confirm(anyString())).willThrow(new RecoveryTokenExpiredException());

        mockMvc.perform(post("/api/client/recovery/confirm").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"raw-token\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.code").value("RECOVERY_TOKEN_EXPIRED"));
    }

    @Test
    void confirm_이미소비된토큰_409() throws Exception {
        given(recoveryConfirmationService.confirm(anyString())).willThrow(new RecoveryTokenInvalidException());

        mockMvc.perform(post("/api/client/recovery/confirm").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"raw-token\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.code").value("RECOVERY_TOKEN_INVALID"));
    }
}
