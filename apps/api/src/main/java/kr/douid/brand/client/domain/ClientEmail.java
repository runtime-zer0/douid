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
 * 검증 완료된 이메일 Recovery Channel
 *
 * 로그인 계정이 아니며, 코드 검증이 성공한 시점에만 생성된다. 등록 요청만 하고 아직 코드 검증
 * 전인 상태는 {@link EmailVerificationChallenge}가 별도로 표현하고 이 엔티티에는 반영되지 않는다.
 */
@Getter
@Entity
@Table(name = "client_email")
public class ClientEmail extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_identity_id", nullable = false)
    private Long clientIdentityId;

    @Column(nullable = false)
    private String email;

    @Column(name = "normalized_email", nullable = false)
    private String normalizedEmail;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    protected ClientEmail() {
    }

    private ClientEmail(Long clientIdentityId, String email, String normalizedEmail, LocalDateTime verifiedAt) {
        this.clientIdentityId = clientIdentityId;
        this.email = email;
        this.normalizedEmail = normalizedEmail;
        this.verifiedAt = verifiedAt;
    }

    /**
     * 검증 완료된 이메일을 생성
     *
     * @param clientIdentityId 이 이메일을 소유하는 상담 주체 ID
     * @param email             사용자 입력 원본 이메일(표시용)
     * @param normalizedEmail   정규화된 이메일
     * @param verifiedAt        검증 완료 시각
     * @return 검증 완료 상태로 생성된 이메일
     */
    public static ClientEmail verify(Long clientIdentityId, String email, String normalizedEmail,
            LocalDateTime verifiedAt) {
        return new ClientEmail(clientIdentityId, email, normalizedEmail, verifiedAt);
    }

    /**
     * 주어진 상담 주체가 이 이메일의 소유자인지 확인
     *
     * @param clientIdentityId 비교할 상담 주체 ID
     * @return 소유자이면 true
     */
    public boolean isOwnedBy(Long clientIdentityId) {
        return this.clientIdentityId.equals(clientIdentityId);
    }
}
