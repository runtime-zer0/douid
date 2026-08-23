package kr.douid.brand.chat.domain;

import kr.douid.brand.shared.exception.DomainException;

/**
 * 종료(CLOSED)된 Conversation에 메시지를 보내려 할 때 발생하는 예외
 */
public class ConversationClosedException extends DomainException {

    /**
     * 기본 메시지로 예외 생성
     */
    public ConversationClosedException() {
        super(ChatErrorCode.CONVERSATION_CLOSED.getType(),
                ChatErrorCode.CONVERSATION_CLOSED.getCode(),
                ChatErrorCode.CONVERSATION_CLOSED.getDefaultMessage());
    }
}
