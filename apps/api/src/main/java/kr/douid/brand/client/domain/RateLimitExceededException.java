package kr.douid.brand.client.domain;

import kr.douid.brand.shared.exception.DomainErrorType;
import kr.douid.brand.shared.exception.DomainException;

/**
 * 인증 코드 발송 또는 Magic Link 발송 요청이 rate limit을 초과했을 때 발생하는 예외
 *
 * 어느 용도에서 발생했는지 구분하지 않는다 — 클라이언트 관점에서는 단일 타입으로 충분하다.
 */
public class RateLimitExceededException extends DomainException {

    public RateLimitExceededException() {
        super(DomainErrorType.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
    }
}
