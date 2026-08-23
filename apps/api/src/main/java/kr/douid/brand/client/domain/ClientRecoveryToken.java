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
 * Magic Link에 사용하는 짧은 수명의 일회성 Recovery Credential
 *
 * {@code ClientIdentity}/{@code ClientEmail}을 ID reference로만 참조하며, Conversation/Message는
 * 어떤 형태로도 참조하지 않는다(FR-034, Chat 비의존 원칙).
 */
@Getter
@Entity
@Table(name = "client_recovery_token")
public class ClientRecoveryToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_identity_id", nullable = false)
    private Long clientIdentityId;

    @Column(name = "client_email_id", nullable = false)
    private Long clientEmailId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    protected ClientRecoveryToken() {
    }

    private ClientRecoveryToken(Long clientIdentityId, Long clientEmailId, String tokenHash,
            LocalDateTime expiresAt) {
        this.clientIdentityId = clientIdentityId;
        this.clientEmailId = clientEmailId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    /**
     * 새 Recovery Token을 발급
     *
     * @param clientIdentityId 복원 대상 상담 주체 ID
     * @param clientEmailId     발급 근거가 된 검증된 이메일 ID
     * @param tokenHash         raw token의 해시 값
     * @param expiresAt         만료 시각
     * @return 발급된 Recovery Token
     */
    public static ClientRecoveryToken issue(Long clientIdentityId, Long clientEmailId, String tokenHash,
            LocalDateTime expiresAt) {
        return new ClientRecoveryToken(clientIdentityId, clientEmailId, tokenHash, expiresAt);
    }

    /**
     * 토큰을 소비 처리
     *
     * @param now 기준 시각
     * @throws RecoveryTokenInvalidException 이미 소비된 경우
     * @throws RecoveryTokenExpiredException 만료된 경우
     */
    public void consume(LocalDateTime now) {
        if (consumedAt != null) {
            throw new RecoveryTokenInvalidException();
        }
        if (expiresAt.isBefore(now)) {
            throw new RecoveryTokenExpiredException();
        }
        consumedAt = now;
    }
}
