package kr.douid.brand.chat.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.douid.brand.chat.domain.Conversation;
import kr.douid.brand.chat.domain.ConversationStatus;

/**
 * {@link Conversation} Spring Data JPA 기술 전용 repository
 */
public interface ConversationJpaRepository extends JpaRepository<Conversation, Long> {

    /**
     * 공개 식별자로 상담을 조회
     *
     * @param publicId 조회할 상담의 공개 식별자
     * @return 상담 (없으면 empty)
     */
    Optional<Conversation> findByPublicId(UUID publicId);

    /**
     * 상담 주체와 상태로 상담을 조회
     *
     * 동시 요청 직렬화는 아직 존재하지 않는 이 행이 아니라
     * {@link kr.douid.brand.client.infrastructure.persistence.ClientIdentityJpaRepository#findByIdForUpdate}의
     * 상담 주체 행 락으로 처리한다.
     *
     * @param clientIdentityId 상담 주체 ID
     * @param status            조회할 상태
     * @return 상담 (없으면 empty)
     */
    Optional<Conversation> findByClientIdentityIdAndStatus(Long clientIdentityId, ConversationStatus status);
}
