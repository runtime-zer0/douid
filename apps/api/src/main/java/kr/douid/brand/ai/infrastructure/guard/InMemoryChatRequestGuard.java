package kr.douid.brand.ai.infrastructure.guard;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;
import kr.douid.brand.ai.application.AiChatException;
import kr.douid.brand.ai.application.AiChatPolicy;
import kr.douid.brand.ai.application.port.ChatRequestGuard;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/** 프로세스 전역 고정 창과 실행·연결 점유 제한 */
public final class InMemoryChatRequestGuard implements ChatRequestGuard {
    private final AiChatPolicy policy;
    private final LongSupplier clock;
    private final MeterRegistry metrics;
    private long windowStart;
    private int attempts;
    private int active;

    /** 전역 guard 생성
     * @param policy 시작 시 고정된 제한 정책
     * @param clock 단조 증가 나노초 clock
     */
    public InMemoryChatRequestGuard(AiChatPolicy policy, LongSupplier clock) {
        this(policy, clock, new SimpleMeterRegistry());
    }

    /** 기존 guard에 비식별 관측 연결
     * @param policy 시작 시 고정된 제한 정책
     * @param clock 단조 증가 clock
     * @param metrics 관측 registry
     */
    public InMemoryChatRequestGuard(AiChatPolicy policy, LongSupplier clock, MeterRegistry metrics) {
        this.policy = policy;
        this.clock = clock;
        this.metrics = metrics;
        this.windowStart = clock.getAsLong();
        metrics.gauge("ai.chat.active", this, InMemoryChatRequestGuard::activeCount);
    }

    /** 고정 창의 요청 시도 한 번 소비 */
    @Override
    public synchronized void attempt() {
        metrics.counter("ai.chat.attempts").increment();
        long elapsed = clock.getAsLong() - windowStart;
        long window = policy.rateWindow().toNanos();
        if (elapsed >= window) {
            windowStart += (elapsed / window) * window;
            attempts = 0;
            elapsed %= window;
        }
        if (attempts >= policy.rateRequests()) {
            metrics.counter("ai.chat.rejected", "reason", "rate").increment();
            long remaining = window - elapsed;
            throw AiChatException.limited(true, Math.max(1, 1 + (remaining - 1) / 1_000_000_000L));
        }
        attempts++;
    }

    /** 실행·연결 점유 획득
     * @return 한 번만 반환되는 점유
     */
    @Override
    public synchronized Lease acquire() {
        if (active >= policy.maxConcurrentStreams()) {
            metrics.counter("ai.chat.rejected", "reason", "concurrency").increment();
            throw AiChatException.limited(false, 1);
        }
        active++;
        metrics.counter("ai.chat.admitted").increment();
        AtomicBoolean returned = new AtomicBoolean();
        return () -> {
            if (returned.compareAndSet(false, true)) release();
        };
    }

    private synchronized void release() { active--; }
    private synchronized double activeCount() { return active; }
}
