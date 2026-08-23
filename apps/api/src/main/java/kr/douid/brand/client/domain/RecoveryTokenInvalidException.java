package kr.douid.brand.client.domain;

import kr.douid.brand.shared.exception.DomainErrorType;
import kr.douid.brand.shared.exception.DomainException;

/**
 * Recovery Token이 존재하지 않거나 이미 소비되었을 때 발생하는 예외
 */
public class RecoveryTokenInvalidException extends DomainException {

    public RecoveryTokenInvalidException() {
        super(DomainErrorType.CONFLICT, "RECOVERY_TOKEN_INVALID", "복원 링크가 유효하지 않거나 이미 사용되었습니다.");
    }
}
