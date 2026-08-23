package kr.douid.brand.client.application;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.douid.brand.client.application.port.EmailSender;
import kr.douid.brand.client.application.port.RateLimiter;
import kr.douid.brand.client.domain.ClientEmail;
import kr.douid.brand.client.domain.ClientEmailRepository;
import kr.douid.brand.client.domain.EmailAlreadyOwnedException;
import kr.douid.brand.client.domain.EmailVerificationChallenge;
import kr.douid.brand.client.domain.EmailVerificationChallengeRepository;
import kr.douid.brand.client.domain.RateLimitExceededException;
import lombok.RequiredArgsConstructor;

/**
 * Recovery Email 최초 등록 요청 유스케이스
 *
 * 현재 상담 주체가 이메일을 입력하면 rate limit을 확인한 뒤 인증 코드를 발급·발송한다(FR-002~004, FR-032).
 * 이미 다른 상담 주체가 검증 완료한 이메일이면 자동 이전하지 않고 거부한다(FR-012).
 */
@Service
@RequiredArgsConstructor
public class ClientEmailRegistrationService {

    private static final int RATE_LIMIT_COUNT = 5;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);
    private static final long CODE_EXPIRATION_MINUTES = 5;

    private final ClientEmailRepository clientEmailRepository;
    private final EmailVerificationChallengeRepository emailVerificationChallengeRepository;
    private final EmailSender emailSender;
    private final RateLimiter rateLimiter;
    private final EmailNormalizer emailNormalizer;
    private final EmailVerificationCodeIssuer emailVerificationCodeIssuer;

    /**
     * Recovery Email 등록을 요청하고 인증 코드를 발송
     *
     * @param clientIdentityId 요청 상담 주체 ID
     * @param email             등록할 이메일 주소
     * @return 이미 본인 소유로 검증 완료된 이메일이라 재발송을 건너뛴 경우 true
     * @throws RateLimitExceededException rate limit을 초과한 경우
     * @throws EmailAlreadyOwnedException  다른 상담 주체가 이미 검증 완료한 이메일인 경우
     */
    @Transactional
    public boolean register(Long clientIdentityId, String email) {
        String normalizedEmail = emailNormalizer.normalize(email);

        if (!rateLimiter.tryConsume(rateLimitKey(normalizedEmail), RATE_LIMIT_COUNT, RATE_LIMIT_WINDOW)) {
            throw new RateLimitExceededException();
        }

        Optional<ClientEmail> ownedBySelf = clientEmailRepository
                .findVerifiedByClientIdentityIdAndNormalizedEmail(clientIdentityId, normalizedEmail);
        if (ownedBySelf.isPresent()) {
            return true;
        }

        clientEmailRepository.findByNormalizedEmail(normalizedEmail)
                .ifPresent(existing -> {
                    throw new EmailAlreadyOwnedException();
                });

        String rawCode = emailVerificationCodeIssuer.issueRawCode();
        String codeHash = emailVerificationCodeIssuer.hash(rawCode);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(CODE_EXPIRATION_MINUTES);

        emailVerificationChallengeRepository.save(
                EmailVerificationChallenge.issue(clientIdentityId, normalizedEmail, codeHash, expiresAt));
        emailSender.sendVerificationCode(email, rawCode);

        return false;
    }

    private String rateLimitKey(String normalizedEmail) {
        return "rate-limit:email-verification:" + normalizedEmail;
    }
}
