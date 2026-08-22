package kr.douid.brand.auth.domain;

import kr.douid.brand.shared.exception.DomainException;

/**
 * 인증된 이메일에 해당하는 관리자 계정을 찾을 수 없을 때 발생하는 예외
 *
 * 세션은 유효하지만 조회 시점에 계정이 삭제·변경된 경우에 발생한다
 */
public class AdminNotFoundException extends DomainException {

    /**
     * 기본 메시지로 예외 생성
     */
    public AdminNotFoundException() {
        super(AuthErrorCode.AUTHENTICATION_FAILED.getType(),
                AuthErrorCode.AUTHENTICATION_FAILED.getCode(),
                AuthErrorCode.AUTHENTICATION_FAILED.getDefaultMessage());
    }
}
