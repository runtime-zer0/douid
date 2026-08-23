package kr.douid.brand.client.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.douid.brand.client.application.port.EmailSender;
import kr.douid.brand.client.application.port.RateLimiter;
import kr.douid.brand.client.domain.ClientEmail;
import kr.douid.brand.client.domain.ClientEmailRepository;
import kr.douid.brand.client.domain.ClientRecoveryToken;
import kr.douid.brand.client.domain.ClientRecoveryTokenRepository;
import kr.douid.brand.client.domain.RateLimitExceededException;

@ExtendWith(MockitoExtension.class)
class RecoveryRequestServiceTest {

    @Mock
    private ClientEmailRepository clientEmailRepository;

    @Mock
    private ClientRecoveryTokenRepository clientRecoveryTokenRepository;

    @Mock
    private EmailSender emailSender;

    @Mock
    private RateLimiter rateLimiter;

    private final EmailNormalizer emailNormalizer = new EmailNormalizer();
    private final RecoveryTokenIssuer recoveryTokenIssuer = new RecoveryTokenIssuer(new ClientCredentialIssuer());
    private final RecoveryProperties recoveryProperties =
            new RecoveryProperties(15, "http://localhost:3000/recovery/confirm");

    private RecoveryRequestService service;

    @BeforeEach
    void setUp() {
        service = new RecoveryRequestService(clientEmailRepository, clientRecoveryTokenRepository, emailSender,
                rateLimiter, emailNormalizer, recoveryTokenIssuer, recoveryProperties);
    }

    @Test
    void request_등록된이메일_MagicLink가발송된다() {
        given(rateLimiter.tryConsume(anyString(), anyInt(), any(Duration.class))).willReturn(true);
        ClientEmail clientEmail = ClientEmail.verify(1L, "user@example.com", "user@example.com",
                LocalDateTime.now());
        given(clientEmailRepository.findByNormalizedEmail("user@example.com")).willReturn(Optional.of(clientEmail));

        service.request("user@example.com");

        verify(emailSender).sendRecoveryMagicLink(anyString(), anyString());
        verify(clientRecoveryTokenRepository).save(any(ClientRecoveryToken.class));
    }

    @Test
    void request_미등록이메일_내부적으로아무것도발송하지않는다() {
        given(rateLimiter.tryConsume(anyString(), anyInt(), any(Duration.class))).willReturn(true);
        given(clientEmailRepository.findByNormalizedEmail("unknown@example.com")).willReturn(Optional.empty());

        service.request("unknown@example.com");

        verify(emailSender, never()).sendRecoveryMagicLink(anyString(), anyString());
        verify(clientRecoveryTokenRepository, never()).save(any());
    }

    @Test
    void request_rateLimit초과_RateLimitExceededException() {
        given(rateLimiter.tryConsume(anyString(), anyInt(), any(Duration.class))).willReturn(false);

        assertThatThrownBy(() -> service.request("user@example.com"))
                .isInstanceOf(RateLimitExceededException.class);

        verify(emailSender, never()).sendRecoveryMagicLink(anyString(), anyString());
    }
}
