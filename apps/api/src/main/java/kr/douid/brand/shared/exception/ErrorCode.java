package kr.douid.brand.shared.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 공통 오류 코드 정의
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "요청 값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다."),

    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "카테고리를 찾을 수 없습니다."),
    CATEGORY_SLUG_DUPLICATE(HttpStatus.CONFLICT, "CATEGORY_SLUG_DUPLICATE", "이미 사용 중인 슬러그입니다."),
    CATEGORY_HAS_WORKS(HttpStatus.CONFLICT, "CATEGORY_HAS_WORKS", "작업물이 연결된 카테고리는 삭제할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String defaultMessage;
}
