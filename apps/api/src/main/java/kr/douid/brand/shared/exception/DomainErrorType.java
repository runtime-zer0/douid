package kr.douid.brand.shared.exception;

/**
 * 도메인 예외의 응답 성격
 *
 * HTTP 상태 코드는 presentation 경계의 handler에서 이 타입을 기준으로 변환한다.
 */
public enum DomainErrorType {
    BAD_REQUEST,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    INTERNAL_ERROR
}
