package kr.douid.brand.shared.exception;

import lombok.Getter;

/**
 * 도메인 규칙 위반을 나타내는 추상 예외
 *
 * feature별 구체 예외가 이 타입을 상속하고, presentation 계층은
 * 이 상위 타입 기준으로 일괄 처리한다.
 */
@Getter
public abstract class DomainException extends RuntimeException {

    private final DomainErrorType type;
    private final String code;

    /**
     * 도메인 예외 생성
     *
     * @param type    도메인 오류 성격
     * @param code    오류 코드
     * @param message 응답에 노출할 오류 메시지
     */
    protected DomainException(DomainErrorType type, String code, String message) {
        super(message);
        this.type = type;
        this.code = code;
    }
}
