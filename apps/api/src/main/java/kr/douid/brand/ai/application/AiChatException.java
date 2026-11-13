package kr.douid.brand.ai.application;

import kr.douid.brand.shared.exception.DomainErrorType;
import kr.douid.brand.shared.exception.DomainException;
import kr.douid.brand.shared.exception.IntegrationErrorType;
import kr.douid.brand.shared.exception.IntegrationException;

/** AI Chat의 안전한 입력·수락 오류 */
public final class AiChatException extends DomainException {
    private AiChatException(DomainErrorType type, String code, String message, Long retryAfter) {
        super(type, code, message, retryAfter);
    }
    /** 입력 오류 생성
     * @return 원문 없는 검증 오류
     */
    public static AiChatException invalid() {
        return new AiChatException(DomainErrorType.BAD_REQUEST, "INVALID_INPUT", "요청 값이 올바르지 않습니다.", null);
    }
    /** 수락 한도 오류 생성
     * @param rate 빈도 한도 여부
     * @param retryAfter 재시도 힌트 초
     * @return 안전한 한도 오류
     */
    public static AiChatException limited(boolean rate, long retryAfter) {
        return new AiChatException(DomainErrorType.TOO_MANY_REQUESTS, rate ? "AI_RATE_LIMITED" : "AI_BUSY",
                "요청이 많습니다. 잠시 후 다시 시도해 주세요.", retryAfter);
    }
    /** AI 처리기 미준비 오류 */
    public static final class Unavailable extends IntegrationException {
        /** 처리기 미준비 오류 생성 */
        public Unavailable() {
            super(IntegrationErrorType.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE", "AI 안내 기능을 사용할 수 없습니다.");
        }
    }
    /** 응답 커밋 전 기한 초과 오류 */
    public static final class Timeout extends IntegrationException {
        /** 기한 초과 오류 생성 */
        public Timeout() {
            super(IntegrationErrorType.GATEWAY_TIMEOUT, "AI_TIMEOUT", "응답 시간이 초과되었습니다.");
        }
    }
}
