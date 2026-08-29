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
     * @param clientIdentityId 상담 주체 ID
     * @param status            조회할 상태
     * @return 상담 (없으면 empty)
     */
    Optional<Conversation> findByClientIdentityIdAndStatus(Long clientIdentityId, ConversationStatus status);
}
