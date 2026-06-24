package kr.douid.brand.shared.presentation;

import org.springframework.http.HttpStatus;

import kr.douid.brand.shared.exception.DomainErrorType;
import kr.douid.brand.shared.exception.IntegrationErrorType;

/**
 * 내부 오류 타입을 HTTP 상태로 변환하는 presentation mapper
 */
final class ErrorHttpStatusMapper {

    private ErrorHttpStatusMapper() {
    }

    static HttpStatus from(DomainErrorType type) {
        return switch (type) {
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    static HttpStatus from(IntegrationErrorType type) {
        return switch (type) {
            case BAD_GATEWAY -> HttpStatus.BAD_GATEWAY;
            case SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case GATEWAY_TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
