package kr.douid.brand.chat.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.douid.brand.shared.entity.BaseTimeEntity;
import lombok.Getter;

/**
 * 상담 세션 Aggregate Root
 *
 * 소유자는 항상 상담 주체({@code clientIdentityId})이며, 내부 식별자({@code id})와
 * 외부 REST 식별자({@code publicId})를 구분한다. {@code publicId}는 authorization 수단이 아니다.
 */
@Getter
@Entity
@Table(name = "conversation")
public class Conversation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;

    @Column(name = "client_identity_id", nullable = false)
    private Long clientIdentityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConversationStatus status;

    protected Conversation() {
    }

    private Conversation(UUID publicId, Long clientIdentityId, ConversationStatus status) {
        this.publicId = publicId;
        this.clientIdentityId = clientIdentityId;
        this.status = status;
    }

    /**
     * 새 활성 상담을 시작
     *
     * @param clientIdentityId 상담 소유자 ID
     * @return 생성된 OPEN 상태의 상담
     */
    public static Conversation open(Long clientIdentityId) {
        return new Conversation(UUID.randomUUID(), clientIdentityId, ConversationStatus.OPEN);
    }

    /**
     * 상담을 종료
     *
     * OPEN 상태일 때만 CLOSED로 전이한다. 이번 Phase는 이 메서드를 호출하는 API를
     * 노출하지 않지만, 후속 AI/Admin handoff Phase가 재사용할 수 있도록 규칙만 정의한다.
     */
    public void close() {
        if (status == ConversationStatus.OPEN) {
            this.status = ConversationStatus.CLOSED;
        }
    }

    /**
     * 소유권을 확인
     *
     * @param clientIdentityId 비교할 상담 주체 ID
     * @return 소유자와 일치하면 true
     */
    public boolean isOwnedBy(Long clientIdentityId) {
        return this.clientIdentityId.equals(clientIdentityId);
    }

    /**
     * 활성 상태 여부를 확인
     *
     * @return OPEN 상태면 true
     */
    public boolean isOpen() {
        return status == ConversationStatus.OPEN;
    }
}
