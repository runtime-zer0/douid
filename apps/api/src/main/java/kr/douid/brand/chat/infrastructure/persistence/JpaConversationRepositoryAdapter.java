package kr.douid.brand.chat.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import kr.douid.brand.chat.domain.Conversation;
import kr.douid.brand.chat.domain.ConversationRepository;
import kr.douid.brand.chat.domain.ConversationStatus;
import lombok.RequiredArgsConstructor;

/**
 * {@link ConversationRepository} domain port의 JPA 구현체
 *
 * {@link ConversationJpaRepository} 위임 호출, JpaRepository 직접 주입 방지
 */
@Repository
@RequiredArgsConstructor
public class JpaConversationRepositoryAdapter implements ConversationRepository {

    private final ConversationJpaRepository conversationJpaRepository;

    /**
     * 상담을 저장하고 반환
     *
     * @param conversation 저장할 상담
     * @return 저장된 상담
     */
    @Override
    public Conversation save(Conversation conversation) {
        return conversationJpaRepository.save(conversation);
    }

    /**
     * 공개 식별자로 상담을 조회
     *
     * @param publicId 조회할 상담의 공개 식별자
     * @return 상담 (없으면 empty)
     */
    @Override
    public Optional<Conversation> findByPublicId(UUID publicId) {
        return conversationJpaRepository.findByPublicId(publicId);
    }

    /**
     * 상담 주체의 활성(OPEN) 상담을 조회
     *
     * @param clientIdentityId 상담 주체 ID
     * @return 활성 상담 (없으면 empty)
     */
    @Override
    public Optional<Conversation> findOpenByClientIdentityId(Long clientIdentityId) {
        return conversationJpaRepository.findByClientIdentityIdAndStatus(clientIdentityId, ConversationStatus.OPEN);
    }
}
