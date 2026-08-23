package kr.douid.brand.chat.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * {@link Conversation} Aggregate 저장·복원을 위한 domain repository port
 */
public interface ConversationRepository {

    /**
     * 상담을 저장하고 반환
     *
     * @param conversation 저장할 상담
     * @return 저장된 상담
     */
    Conversation save(Conversation conversation);

    /**
     * 공개 식별자로 상담을 조회
     *
     * @param publicId 조회할 상담의 공개 식별자
     * @return 상담 (없으면 empty)
     */
    Optional<Conversation> findByPublicId(UUID publicId);

    /**
     * 상담 주체의 활성(OPEN) 상담을 조회
     *
     * @param clientIdentityId 상담 주체 ID
     * @return 활성 상담 (없으면 empty)
     */
    Optional<Conversation> findOpenByClientIdentityId(Long clientIdentityId);
}
