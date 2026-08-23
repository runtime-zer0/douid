package kr.douid.brand.client.domain;

import kr.douid.brand.shared.exception.DomainErrorType;
import kr.douid.brand.shared.exception.DomainException;

/**
 * Recovery Token이 만료되었을 때 발생하는 예외
 */
public class RecoveryTokenExpiredException extends DomainException {

    public RecoveryTokenExpiredException() {
        super(DomainErrorType.CONFLICT, "RECOVERY_TOKEN_EXPIRED", "복원 링크가 만료되었습니다. 다시 요청해주세요.");
    }
}
