package kr.douid.brand.chat.domain;

import kr.douid.brand.shared.exception.DomainException;

/**
 * Conversation을 찾을 수 없을 때 발생하는 예외
 */
public class ConversationNotFoundException extends DomainException {

    /**
     * 기본 메시지로 예외 생성
     */
    public ConversationNotFoundException() {
        super(ChatErrorCode.CONVERSATION_NOT_FOUND.getType(),
                ChatErrorCode.CONVERSATION_NOT_FOUND.getCode(),
                ChatErrorCode.CONVERSATION_NOT_FOUND.getDefaultMessage());
    }
}
