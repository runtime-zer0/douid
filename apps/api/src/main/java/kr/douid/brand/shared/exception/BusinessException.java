package kr.douid.brand.shared.exception;

import lombok.Getter;

/**
 * 도메인 비즈니스 규칙 위반을 나타내는 런타임 예외
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * 오류 코드 기본 메시지 기반 예외 생성
     *
     * @param errorCode 오류 코드
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    /**
     * 사용자 정의 메시지 기반 예외 생성
     *
     * @param errorCode 오류 코드
     * @param message   응답에 노출할 구체적인 오류 메시지
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
