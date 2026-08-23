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
 * 특정 브라우저가 어떤 상담 주체에 연결되어 있는지 증명하는 credential
 *
 * {@link ClientIdentity}와는 독립된 생명주기를 가지며, ID reference로만 연결한다(JPA 연관관계 없음).
 */
@Getter
@Entity
@Table(name = "client_credential")
public class ClientCredential extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_identity_id", nullable = false)
    private Long clientIdentityId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    protected ClientCredential() {
    }

    private ClientCredential(Long clientIdentityId, String tokenHash, LocalDateTime expiresAt) {
        this.clientIdentityId = clientIdentityId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.revoked = false;
    }

    /**
     * 새 credential을 발급
     *
     * @param clientIdentityId 이 credential이 연결될 상담 주체 ID
     * @param tokenHash         raw token의 해시 값
     * @param expiresAt         만료 시각
     * @return 발급된 credential
     */
    public static ClientCredential issue(Long clientIdentityId, String tokenHash, LocalDateTime expiresAt) {
        return new ClientCredential(clientIdentityId, tokenHash, expiresAt);
    }

    /**
     * credential을 폐기
     */
    public void revoke() {
        this.revoked = true;
    }

    /**
     * credential 유효성을 확인
     *
     * @param now 기준 시각
     * @return 폐기되지 않았고 만료되지 않았으면 true
     */
    public boolean isValid(LocalDateTime now) {
        return !revoked && expiresAt.isAfter(now);
    }
}
