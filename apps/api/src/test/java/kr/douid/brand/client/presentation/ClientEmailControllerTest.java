package kr.douid.brand.client.presentation;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import kr.douid.brand.auth.application.AuthenticationService;
import kr.douid.brand.client.application.ClientAuthenticationService;
import kr.douid.brand.client.application.ClientEmailRegistrationService;
import kr.douid.brand.client.application.ClientEmailVerificationService;
import kr.douid.brand.client.domain.ClientEmail;
import kr.douid.brand.client.domain.EmailAlreadyOwnedException;
import kr.douid.brand.client.domain.RateLimitExceededException;
import kr.douid.brand.client.domain.VerificationCodeExpiredException;
import kr.douid.brand.shared.config.SecurityConfig;
import kr.douid.brand.shared.presentation.GlobalExceptionHandler;

@WebMvcTest(ClientEmailController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@WithAnonymousUser
class ClientEmailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private ClientAuthenticationService clientAuthenticationService;

    @MockitoBean
    private ClientEmailRegistrationService clientEmailRegistrationService;

    @MockitoBean
    private ClientEmailVerificationService clientEmailVerificationService;

    @Test
    void register_유효한클라이언트토큰_202() throws Exception {
        given(clientAuthenticationService.resolve(anyString())).willReturn(Optional.of(1L));
        given(clientEmailRegistrationService.register(eq(1L), anyString())).willReturn(false);

        mockMvc.perform(post("/api/client/emails").with(csrf())
                        .cookie(new Cookie("client_token", "valid-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.alreadyVerified").value(false));
    }

    @Test
    void register_클라이언트토큰없음_401() throws Exception {
        mockMvc.perform(post("/api/client/emails").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_이메일형식오류_400() throws Exception {
        given(clientAuthenticationService.resolve(anyString())).willReturn(Optional.of(1L));

        mockMvc.perform(post("/api/client/emails").with(csrf())
                        .cookie(new Cookie("client_token", "valid-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalid-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_이미다른상담주체가소유한이메일_409() throws Exception {
        given(clientAuthenticationService.resolve(anyString())).willReturn(Optional.of(1L));
        given(clientEmailRegistrationService.register(eq(1L), anyString()))
                .willThrow(new EmailAlreadyOwnedException());

        mockMvc.perform(post("/api/client/emails").with(csrf())
                        .cookie(new Cookie("client_token", "valid-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.code").value("EMAIL_ALREADY_OWNED"));
    }

    @Test
    void register_rateLimit초과_429() throws Exception {
        given(clientAuthenticationService.resolve(anyString())).willReturn(Optional.of(1L));
        given(clientEmailRegistrationService.register(eq(1L), anyString()))
                .willThrow(new RateLimitExceededException());

        mockMvc.perform(post("/api/client/emails").with(csrf())
                        .cookie(new Cookie("client_token", "valid-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.data.code").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void verify_정상검증_200() throws Exception {
        given(clientAuthenticationService.resolve(anyString())).willReturn(Optional.of(1L));
        given(clientEmailVerificationService.verify(eq(1L), anyString(), anyString()))
                .willReturn(ClientEmail.verify(1L, "user@example.com", "user@example.com", LocalDateTime.now()));

        mockMvc.perform(post("/api/client/emails/verify").with(csrf())
                        .cookie(new Cookie("client_token", "valid-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("user@example.com"));
    }

    @Test
    void verify_코드형식오류_400() throws Exception {
        given(clientAuthenticationService.resolve(anyString())).willReturn(Optional.of(1L));

        mockMvc.perform(post("/api/client/emails/verify").with(csrf())
                        .cookie(new Cookie("client_token", "valid-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"code\":\"12\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verify_클라이언트토큰없음_401() throws Exception {
        mockMvc.perform(post("/api/client/emails/verify").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"code\":\"123456\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verify_만료된코드_409() throws Exception {
        given(clientAuthenticationService.resolve(anyString())).willReturn(Optional.of(1L));
        given(clientEmailVerificationService.verify(eq(1L), anyString(), anyString()))
                .willThrow(new VerificationCodeExpiredException());

        mockMvc.perform(post("/api/client/emails/verify").with(csrf())
                        .cookie(new Cookie("client_token", "valid-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"code\":\"123456\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.code").value("VERIFICATION_CODE_EXPIRED"));
    }
}
