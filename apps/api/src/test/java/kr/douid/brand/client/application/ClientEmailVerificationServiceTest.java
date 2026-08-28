package kr.douid.brand.client.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import kr.douid.brand.client.domain.ClientEmail;
import kr.douid.brand.client.domain.ClientEmailRepository;
import kr.douid.brand.client.domain.EmailAlreadyOwnedException;
import kr.douid.brand.client.domain.EmailVerificationChallenge;
import kr.douid.brand.client.domain.EmailVerificationChallengeRepository;
import kr.douid.brand.client.domain.VerificationCodeExpiredException;
import kr.douid.brand.client.domain.VerificationCodeInvalidException;

@ExtendWith(MockitoExtension.class)
class ClientEmailVerificationServiceTest {

    @Mock
    private ClientEmailRepository clientEmailRepository;

    @Mock
    private EmailVerificationChallengeRepository emailVerificationChallengeRepository;

    private final EmailNormalizer emailNormalizer = new EmailNormalizer();
    private final EmailVerificationCodeIssuer codeIssuer =
            new EmailVerificationCodeIssuer(new ClientCredentialIssuer());

    private ClientEmailVerificationService service;

    @BeforeEach
    void setUp() {
        service = new ClientEmailVerificationService(clientEmailRepository,
                emailVerificationChallengeRepository, emailNormalizer, codeIssuer);
    }

    @Test
    void verify_정상검증_ClientEmail이_생성되고_verifiedAt이_기록된다() {
        String rawCode = "123456";
        EmailVerificationChallenge challenge = EmailVerificationChallenge.issue(1L, "user@example.com",
                codeIssuer.hash(rawCode), LocalDateTime.now().plusMinutes(5));
        given(emailVerificationChallengeRepository.findLatestByClientIdentityIdAndNormalizedEmail(1L,
                "user@example.com")).willReturn(Optional.of(challenge));
        given(clientEmailRepository.findByNormalizedEmail("user@example.com")).willReturn(Optional.empty());
        given(clientEmailRepository.save(any(ClientEmail.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        ClientEmail result = service.verify(1L, "user@example.com", rawCode);

        assertThat(result.getVerifiedAt()).isNotNull();
        assertThat(result.getClientIdentityId()).isEqualTo(1L);
    }

    @Test
    void verify_코드불일치_VerificationCodeInvalidException() {
        EmailVerificationChallenge challenge = EmailVerificationChallenge.issue(1L, "user@example.com",
                codeIssuer.hash("123456"), LocalDateTime.now().plusMinutes(5));
        given(emailVerificationChallengeRepository.findLatestByClientIdentityIdAndNormalizedEmail(1L,
                "user@example.com")).willReturn(Optional.of(challenge));

        assertThatThrownBy(() -> service.verify(1L, "user@example.com", "000000"))
                .isInstanceOf(VerificationCodeInvalidException.class);
    }

    @Test
    void verify_챌린지없음_VerificationCodeExpiredException() {
        given(emailVerificationChallengeRepository.findLatestByClientIdentityIdAndNormalizedEmail(1L,
                "user@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify(1L, "user@example.com", "123456"))
                .isInstanceOf(VerificationCodeExpiredException.class);
    }

    @Test
    void verify_사전조회에서_이미다른상담주체가검증완료_EmailAlreadyOwnedException() {
        String rawCode = "123456";
        EmailVerificationChallenge challenge = EmailVerificationChallenge.issue(1L, "user@example.com",
                codeIssuer.hash(rawCode), LocalDateTime.now().plusMinutes(5));
        given(emailVerificationChallengeRepository.findLatestByClientIdentityIdAndNormalizedEmail(1L,
                "user@example.com")).willReturn(Optional.of(challenge));
        ClientEmail ownedByOther =
                ClientEmail.verify(2L, "user@example.com", "user@example.com", LocalDateTime.now());
        given(clientEmailRepository.findByNormalizedEmail("user@example.com")).willReturn(Optional.of(ownedByOther));

        assertThatThrownBy(() -> service.verify(1L, "user@example.com", rawCode))
                .isInstanceOf(EmailAlreadyOwnedException.class);
    }

    @Test
    void verify_저장시점경쟁조건으로_unique제약위반_EmailAlreadyOwnedException() {
        String rawCode = "123456";
        EmailVerificationChallenge challenge = EmailVerificationChallenge.issue(1L, "user@example.com",
                codeIssuer.hash(rawCode), LocalDateTime.now().plusMinutes(5));
        given(emailVerificationChallengeRepository.findLatestByClientIdentityIdAndNormalizedEmail(1L,
                "user@example.com")).willReturn(Optional.of(challenge));
        given(clientEmailRepository.findByNormalizedEmail("user@example.com")).willReturn(Optional.empty());
        willThrow(new DataIntegrityViolationException("unique constraint violation"))
                .given(clientEmailRepository).save(any(ClientEmail.class));

        assertThatThrownBy(() -> service.verify(1L, "user@example.com", rawCode))
                .isInstanceOf(EmailAlreadyOwnedException.class);
    }

    @Test
    void verify_등록당시와다른상담주체_challenge조회되지않아검증실패() {
        given(emailVerificationChallengeRepository.findLatestByClientIdentityIdAndNormalizedEmail(2L,
                "user@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify(2L, "user@example.com", "123456"))
                .isInstanceOf(VerificationCodeExpiredException.class);
    }
}
