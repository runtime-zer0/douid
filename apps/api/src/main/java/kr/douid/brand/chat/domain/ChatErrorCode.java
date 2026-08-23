package kr.douid.brand.chat.domain;

import kr.douid.brand.shared.exception.DomainErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 채팅 도메인 오류 코드
 */
@Getter
@RequiredArgsConstructor
public enum ChatErrorCode {

    CONVERSATION_NOT_FOUND(DomainErrorType.NOT_FOUND, "CONVERSATION_NOT_FOUND", "상담을 찾을 수 없습니다."),
    CONVERSATION_ACCESS_DENIED(DomainErrorType.FORBIDDEN, "CONVERSATION_ACCESS_DENIED", "해당 상담에 접근할 권한이 없습니다."),
    CONVERSATION_CLOSED(DomainErrorType.CONFLICT, "CONVERSATION_CLOSED", "이미 종료된 상담에는 메시지를 보낼 수 없습니다.");

    private final DomainErrorType type;
    private final String code;
    private final String defaultMessage;
}
