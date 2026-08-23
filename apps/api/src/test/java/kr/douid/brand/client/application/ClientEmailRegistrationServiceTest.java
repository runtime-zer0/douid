package kr.douid.brand.client.application;

import static org.assertj.core.api.Assertions.assertThat;
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
import kr.douid.brand.client.domain.EmailAlreadyOwnedException;
import kr.douid.brand.client.domain.EmailVerificationChallenge;
import kr.douid.brand.client.domain.EmailVerificationChallengeRepository;
import kr.douid.brand.client.domain.RateLimitExceededException;

@ExtendWith(MockitoExtension.class)
class ClientEmailRegistrationServiceTest {

    @Mock
    private ClientEmailRepository clientEmailRepository;

    @Mock
    private EmailVerificationChallengeRepository emailVerificationChallengeRepository;

    @Mock
    private EmailSender emailSender;

    @Mock
    private RateLimiter rateLimiter;

    private final EmailNormalizer emailNormalizer = new EmailNormalizer();
    private final EmailVerificationCodeIssuer codeIssuer =
            new EmailVerificationCodeIssuer(new ClientCredentialIssuer());

    private ClientEmailRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new ClientEmailRegistrationService(clientEmailRepository,
                emailVerificationChallengeRepository, emailSender, rateLimiter, emailNormalizer, codeIssuer);
    }

    @Test
    void register_정상등록_인증코드가_발송된다() {
        given(rateLimiter.tryConsume(anyString(), anyInt(), any(Duration.class))).willReturn(true);
        given(clientEmailRepository.findVerifiedByClientIdentityIdAndNormalizedEmail(1L, "user@example.com"))
                .willReturn(Optional.empty());
        given(clientEmailRepository.findByNormalizedEmail("user@example.com")).willReturn(Optional.empty());

        service.register(1L, "User@Example.com");

        verify(emailSender).sendVerificationCode(anyString(), anyString());
        verify(emailVerificationChallengeRepository).save(any(EmailVerificationChallenge.class));
    }

    @Test
    void register_이미본인이_검증완료한이메일_중복등록스킵된다() {
        given(rateLimiter.tryConsume(anyString(), anyInt(), any(Duration.class))).willReturn(true);
        ClientEmail existing = ClientEmail.verify(1L, "user@example.com", "user@example.com", LocalDateTime.now());
        given(clientEmailRepository.findVerifiedByClientIdentityIdAndNormalizedEmail(1L, "user@example.com"))
                .willReturn(Optional.of(existing));

        service.register(1L, "user@example.com");

        verify(emailSender, never()).sendVerificationCode(anyString(), anyString());
        verify(emailVerificationChallengeRepository, never()).save(any());
    }

    @Test
    void register_다른상담주체가_이미소유한이메일_EmailAlreadyOwnedException() {
        given(rateLimiter.tryConsume(anyString(), anyInt(), any(Duration.class))).willReturn(true);
        given(clientEmailRepository.findVerifiedByClientIdentityIdAndNormalizedEmail(1L, "user@example.com"))
                .willReturn(Optional.empty());
        ClientEmail ownedByOther =
                ClientEmail.verify(2L, "user@example.com", "user@example.com", LocalDateTime.now());
        given(clientEmailRepository.findByNormalizedEmail("user@example.com")).willReturn(Optional.of(ownedByOther));

        assertThatThrownBy(() -> service.register(1L, "user@example.com"))
                .isInstanceOf(EmailAlreadyOwnedException.class);

        verify(emailSender, never()).sendVerificationCode(anyString(), anyString());
    }

    @Test
    void register_rateLimit초과_RateLimitExceededException() {
        given(rateLimiter.tryConsume(anyString(), anyInt(), any(Duration.class))).willReturn(false);

        assertThatThrownBy(() -> service.register(1L, "user@example.com"))
                .isInstanceOf(RateLimitExceededException.class);

        verify(emailSender, never()).sendVerificationCode(anyString(), anyString());
    }
}
