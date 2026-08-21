package kr.douid.brand.auth.domain;

import kr.douid.brand.shared.exception.DomainException;

/**
 * 로그인 자격 증명 검증에 실패했을 때 발생하는 예외
 *
 * 계정 미존재, 비밀번호 불일치, 비활성 계정을 모두 동일하게 취급한다
 */
public class AuthenticationFailedException extends DomainException {

    /**
     * 기본 메시지로 예외 생성
     */
    public AuthenticationFailedException() {
        super(AuthErrorCode.AUTHENTICATION_FAILED.getType(),
                AuthErrorCode.AUTHENTICATION_FAILED.getCode(),
                AuthErrorCode.AUTHENTICATION_FAILED.getDefaultMessage());
    }
}
