package kr.douid.brand.chat.application;

import java.util.Optional;

import org.springframework.stereotype.Component;

import kr.douid.brand.chat.domain.Conversation;
import kr.douid.brand.chat.domain.ConversationAccessDeniedException;

/**
 * Conversation ownership 검증
 *
 * 인증된 상담 주체와 Conversation 소유자가 일치하는지 확인한다(FR-019, FR-020).
 */
@Component
public class ConversationOwnershipValidator {

    /**
     * 소유권을 검증
     *
     * @param conversation      검증 대상 상담
     * @param clientIdentityId  인증된 상담 주체 ID (미인증이면 empty)
     * @throws ConversationAccessDeniedException 미인증이거나 소유자가 일치하지 않는 경우
     */
    public void validate(Conversation conversation, Optional<Long> clientIdentityId) {
        if (clientIdentityId.isEmpty() || !conversation.isOwnedBy(clientIdentityId.get())) {
            throw new ConversationAccessDeniedException();
        }
    }
}
