package kr.douid.brand.client.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.douid.brand.shared.entity.BaseTimeEntity;
import lombok.Getter;

/**
 * 최초 이메일 연결을 위한 일회성 인증 코드 챌린지
 *
 * 코드 검증에는 발급 당시의 상담 주체 credential이 함께 필요하다(Story 2 방어).
 */
@Getter
@Entity
@Table(name = "email_verification_challenge")
public class EmailVerificationChallenge extends BaseTimeEntity {

    private static final int MAX_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_identity_id", nullable = false)
    private Long clientIdentityId;

    @Column(name = "normalized_email", nullable = false)
    private String normalizedEmail;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    protected EmailVerificationChallenge() {
    }

    private EmailVerificationChallenge(Long clientIdentityId, String normalizedEmail, String codeHash,
            LocalDateTime expiresAt) {
        this.clientIdentityId = clientIdentityId;
        this.normalizedEmail = normalizedEmail;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.attemptCount = 0;
    }

    /**
     * 새 인증 코드 챌린지를 발급
     *
     * @param clientIdentityId 챌린지를 요청한 상담 주체 ID
     * @param normalizedEmail   검증 대상 이메일(정규화)
     * @param codeHash          발급된 코드의 해시
     * @param expiresAt         만료 시각
     * @return 발급된 챌린지
     */
    public static EmailVerificationChallenge issue(Long clientIdentityId, String normalizedEmail, String codeHash,
            LocalDateTime expiresAt) {
        return new EmailVerificationChallenge(clientIdentityId, normalizedEmail, codeHash, expiresAt);
    }

    /**
     * 제출된 코드 해시로 검증을 시도
     *
     * @param candidateHash 제출된 코드의 해시
     * @param now           기준 시각
     * @throws VerificationCodeExpiredException 이미 소비되었거나 만료되었거나 시도 횟수를 초과한 경우
     * @throws VerificationCodeInvalidException 코드가 일치하지 않는 경우
     */
    public void verify(String candidateHash, LocalDateTime now) {
        if (consumedAt != null || expiresAt.isBefore(now) || attemptCount >= MAX_ATTEMPTS) {
            throw new VerificationCodeExpiredException();
        }
        if (!codeHash.equals(candidateHash)) {
            attemptCount++;
            throw new VerificationCodeInvalidException();
        }
        consumedAt = now;
    }

    /**
     * 아직 시도 가능한 상태인지 확인
     *
     * 코드 일치 여부와 무관하게 소비되지 않았고, 만료되지 않았고, 시도 횟수가 남아있는지만 판단한다.
     *
     * @param now 기준 시각
     * @return 시도 가능하면 true
     */
    public boolean isConsumable(LocalDateTime now) {
        return consumedAt == null && expiresAt.isAfter(now) && attemptCount < MAX_ATTEMPTS;
    }
}
