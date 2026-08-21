package kr.douid.brand.auth.domain;

import kr.douid.brand.shared.exception.DomainErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 인증 도메인 오류 코드
 */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode {

    AUTHENTICATION_FAILED(DomainErrorType.UNAUTHORIZED, "AUTHENTICATION_FAILED",
            "이메일 또는 비밀번호가 올바르지 않습니다.");

    private final DomainErrorType type;
    private final String code;
    private final String defaultMessage;
}
