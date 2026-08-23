package kr.douid.brand.client.domain;

import kr.douid.brand.shared.exception.DomainErrorType;
import kr.douid.brand.shared.exception.DomainException;

/**
 * 인증 코드가 만료되었거나, 이미 사용되었거나, 시도 횟수를 초과했을 때 발생하는 예외
 */
public class VerificationCodeExpiredException extends DomainException {

    public VerificationCodeExpiredException() {
        super(DomainErrorType.CONFLICT, "VERIFICATION_CODE_EXPIRED", "인증 코드가 만료되었거나 더 이상 사용할 수 없습니다.");
    }
}
