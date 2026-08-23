package kr.douid.brand.client.domain;

import kr.douid.brand.shared.exception.DomainErrorType;
import kr.douid.brand.shared.exception.DomainException;

/**
 * 이미 다른 상담 주체가 검증 완료한 이메일을 등록하려 할 때 발생하는 예외
 */
public class EmailAlreadyOwnedException extends DomainException {

    public EmailAlreadyOwnedException() {
        super(DomainErrorType.CONFLICT, "EMAIL_ALREADY_OWNED", "이미 다른 상담 주체에 연결된 이메일입니다.");
    }
}
