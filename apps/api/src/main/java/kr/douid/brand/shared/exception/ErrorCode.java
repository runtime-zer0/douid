package kr.douid.brand.shared.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 공통 오류 코드 정의
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT("INVALID_INPUT", "요청 값이 올바르지 않습니다."),
    UNAUTHORIZED("UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN("FORBIDDEN", "접근 권한이 없습니다."),
    NOT_FOUND("NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    CONFLICT("CONFLICT", "요청이 현재 서버 상태와 충돌합니다."),
    INTERNAL_SERVER_ERROR("INTERNAL_ERROR", "서버 오류가 발생했습니다.");

    private final String code;
    private final String defaultMessage;
}
