package kr.douid.brand.chat.domain;

import kr.douid.brand.shared.exception.DomainException;

/**
 * 인증된 상담 주체와 Conversation 소유자가 일치하지 않을 때 발생하는 예외
 */
public class ConversationAccessDeniedException extends DomainException {

    /**
     * 기본 메시지로 예외 생성
     */
    public ConversationAccessDeniedException() {
        super(ChatErrorCode.CONVERSATION_ACCESS_DENIED.getType(),
                ChatErrorCode.CONVERSATION_ACCESS_DENIED.getCode(),
                ChatErrorCode.CONVERSATION_ACCESS_DENIED.getDefaultMessage());
    }
}
