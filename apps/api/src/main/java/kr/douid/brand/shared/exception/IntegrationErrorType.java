package kr.douid.brand.shared.exception;

/**
 * 외부 연동 예외의 응답 성격
 *
 * HTTP 상태 코드는 presentation 경계의 handler에서 이 타입을 기준으로 변환한다.
 */
public enum IntegrationErrorType {
    BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT, INTERNAL_ERROR
}
