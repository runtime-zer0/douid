package kr.douid.brand.shared.exception;

import lombok.Getter;

/**
 * 외부 시스템 연동 실패를 나타내는 추상 예외
 *
 * AI, 스토리지, 메시지 브로커 등 인프라 호출 실패에 사용한다.
 * presentation 계층은 이 상위 타입 기준으로 일괄 처리한다.
 */
@Getter
public abstract class IntegrationException extends RuntimeException {

    private final IntegrationErrorType type;
    private final String code;

    /**
     * 연동 예외 생성
     *
     * @param type    연동 오류 성격
     * @param code    오류 코드
     * @param message 응답에 노출할 오류 메시지
     */
    protected IntegrationException(IntegrationErrorType type, String code, String message) {
        super(message);
        this.type = type;
        this.code = code;
    }

    /**
     * 원인 예외를 포함한 연동 예외 생성
     *
     * @param type    연동 오류 성격
     * @param code    오류 코드
     * @param message 응답에 노출할 오류 메시지
     * @param cause   원인 예외
     */
    protected IntegrationException(IntegrationErrorType type, String code, String message, Throwable cause) {
        super(message, cause);
        this.type = type;
        this.code = code;
    }
}
