package kr.douid.brand.client.application;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.douid.brand.client.application.port.EmailSender;
import kr.douid.brand.client.application.port.RateLimiter;
import kr.douid.brand.client.domain.ClientEmail;
import kr.douid.brand.client.domain.ClientEmailRepository;
import kr.douid.brand.client.domain.ClientRecoveryToken;
import kr.douid.brand.client.domain.ClientRecoveryTokenRepository;
import kr.douid.brand.client.domain.RateLimitExceededException;
import lombok.RequiredArgsConstructor;

/**
 * Magic Link 기반 상담 복원 요청 유스케이스
 *
 * 이메일 등록 여부와 무관하게 항상 동일한 결과를 반환한다(FR-014) — 검증된 이메일일 때만 내부적으로
 * Magic Link를 발급·발송하고, 그렇지 않으면 아무 것도 하지 않은 채 동일하게 반환한다.
 */
@Service
@RequiredArgsConstructor
public class RecoveryRequestService {

    private static final int RATE_LIMIT_COUNT = 5;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);

    private final ClientEmailRepository clientEmailRepository;
    private final ClientRecoveryTokenRepository clientRecoveryTokenRepository;
    private final EmailSender emailSender;
    private final RateLimiter rateLimiter;
    private final EmailNormalizer emailNormalizer;
    private final RecoveryTokenIssuer recoveryTokenIssuer;
    private final RecoveryProperties recoveryProperties;

    /**
     * 검증된 이메일이면 Magic Link를 발급·발송
     *
     * @param email 복원을 요청한 이메일 주소
     * @throws RateLimitExceededException rate limit을 초과한 경우
     */
    @Transactional
    public void request(String email) {
        String normalizedEmail = emailNormalizer.normalize(email);

        if (!rateLimiter.tryConsume(rateLimitKey(normalizedEmail), RATE_LIMIT_COUNT, RATE_LIMIT_WINDOW)) {
            throw new RateLimitExceededException();
        }

        clientEmailRepository.findByNormalizedEmail(normalizedEmail)
                .ifPresent(this::issueAndSendMagicLink);
    }

    private void issueAndSendMagicLink(ClientEmail clientEmail) {
        String rawToken = recoveryTokenIssuer.issueRawToken();
        String tokenHash = recoveryTokenIssuer.hash(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(recoveryProperties.tokenExpirationMinutes());

        clientRecoveryTokenRepository.save(
                ClientRecoveryToken.issue(clientEmail.getClientIdentityId(), clientEmail.getId(), tokenHash,
                        expiresAt));

        String magicLinkUrl = recoveryProperties.magicLinkBaseUrl() + "?token=" + rawToken;
        emailSender.sendRecoveryMagicLink(clientEmail.getEmail(), magicLinkUrl);
    }

    private String rateLimitKey(String normalizedEmail) {
        return "rate-limit:recovery:" + normalizedEmail;
    }
}
