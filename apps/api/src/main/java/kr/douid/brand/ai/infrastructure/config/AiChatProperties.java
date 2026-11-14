package kr.douid.brand.ai.infrastructure.config;

import java.time.Duration;

import kr.douid.brand.ai.application.AiChatPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** AI Chat 환경별 유한 자원 설정 */
@ConfigurationProperties("app.ai-chat")
public record AiChatProperties(
        @DefaultValue("16384") int maxRequestBytes,
        @DefaultValue("2000") int maxMessageCodePoints,
        @DefaultValue("12") int maxHistoryMessages,
        @DefaultValue("2000") int maxHistoryMessageCodePoints,
        @DefaultValue("12000") int maxContextCodePoints,
        @DefaultValue RateLimit rateLimit,
        @DefaultValue("2") int maxConcurrentStreams,
        @DefaultValue("60s") Duration executionTimeout,
        @DefaultValue("65s") Duration emitterTimeout,
        @DefaultValue("65536") long maxOutputBytes,
        @DefaultValue("4096") int maxDeltaEvents) {

    /** 시작 시 유한 자원 정책 검증
     * @param maxRequestBytes 입력 byte 한도
     * @param maxMessageCodePoints 질문 길이 한도
     * @param maxHistoryMessages 이력 개수 한도
     * @param maxHistoryMessageCodePoints 이력별 길이 한도
     * @param maxContextCodePoints 누적 맥락 한도
     * @param rateLimit 빈도 정책
     * @param maxConcurrentStreams 동시 실행·연결 상한
     * @param executionTimeout 실행 기한
     * @param emitterTimeout 연결 기한
     * @param maxOutputBytes 출력 byte 한도
     * @param maxDeltaEvents 출력 이벤트 한도
     * @throws IllegalArgumentException 무효한 제한값
     * @throws ArithmeticException duration 변환 범위 초과
     */
    public AiChatProperties {
        if (maxRequestBytes < 1 || maxRequestBytes == Integer.MAX_VALUE || maxMessageCodePoints < 1
                || maxHistoryMessages < 1 || maxHistoryMessageCodePoints < 1
                || maxContextCodePoints < maxMessageCodePoints || rateLimit == null
                || rateLimit.requests() < 1 || !positive(rateLimit.window())
                || maxConcurrentStreams < 1 || maxConcurrentStreams > 64
                || !positive(executionTimeout) || !positive(emitterTimeout)
                || emitterTimeout.compareTo(executionTimeout) <= 0 || maxOutputBytes < 1 || maxDeltaEvents < 1) {
            throw new IllegalArgumentException("Invalid AI chat resource policy");
        }
        // Duration 사용 시 overflow와 0ms emitter timeout(무제한)을 시작 시 차단
        rateLimit.window().toNanos();
        executionTimeout.toNanos();
        if (emitterTimeout.toMillis() < 1) {
            throw new IllegalArgumentException("Invalid emitter timeout");
        }
    }

    private static boolean positive(Duration duration) {
        return duration != null && !duration.isNegative() && !duration.isZero();
    }

    /**
     * 프레임워크와 독립된 실행 정책 snapshot
     * @return 변경 불가능한 정책
     */
    public AiChatPolicy toPolicy() {
        return new AiChatPolicy(maxRequestBytes, maxMessageCodePoints,
                maxHistoryMessages, maxHistoryMessageCodePoints, maxContextCodePoints,
                rateLimit.requests(), rateLimit.window(), maxConcurrentStreams,
                executionTimeout, emitterTimeout, maxOutputBytes, maxDeltaEvents);
    }

    public record RateLimit(@DefaultValue("10") int requests, @DefaultValue("60s") Duration window) {}
}
