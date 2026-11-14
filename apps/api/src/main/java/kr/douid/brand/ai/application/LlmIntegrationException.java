package kr.douid.brand.ai.application;

import kr.douid.brand.shared.exception.IntegrationException;
import kr.douid.brand.shared.exception.IntegrationErrorType;

/** 원문과 원인 예외를 보관하지 않는 공급자 중립 실패 */
public final class LlmIntegrationException extends IntegrationException {
    public enum Kind {
        AUTHENTICATION, RATE_LIMIT, UNAVAILABLE, CONNECTION, PROVIDER_FAILED,
        CONNECT_TIMEOUT, HEADER_TIMEOUT, COUNT_TIMEOUT, READ_IDLE_TIMEOUT,
        INCOMPLETE_OUTPUT_LIMIT, INCOMPLETE_OTHER, PROTOCOL, EMPTY_OUTPUT,
        UNSUPPORTED_OUTPUT, INPUT_BUDGET_EXCEEDED, PROVIDER_RESOURCE_LIMIT, CLIENT_CANCELLED
    }

    private final Kind kind;

    /** 안전한 실패 분류
     * @param kind 내부 실패 종류
     */
    public LlmIntegrationException(Kind kind) {
        super(IntegrationErrorType.BAD_GATEWAY, "AI_PROVIDER_ERROR", "AI provider request failed");
        this.kind = kind;
    }

    /** 내부 관측용 분류
     * @return 원문 없는 실패 종류
     */
    public Kind kind() { return kind; }
}
