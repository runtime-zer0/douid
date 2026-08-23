package kr.douid.brand.client.domain;

import kr.douid.brand.shared.exception.DomainErrorType;
import kr.douid.brand.shared.exception.DomainException;

/**
 * 제출된 인증 코드가 발급된 코드와 일치하지 않을 때 발생하는 예외
 */
public class VerificationCodeInvalidException extends DomainException {

    public VerificationCodeInvalidException() {
        super(DomainErrorType.CONFLICT, "VERIFICATION_CODE_INVALID", "인증 코드가 올바르지 않습니다.");
    }
}
