package kr.douid.brand.ai.infrastructure.stream;

import java.time.Duration;
import java.util.concurrent.*;
import kr.douid.brand.ai.application.port.ChatStreamExecutor;

/** 대기열 없는 고정 worker와 비차단 deadline scheduler */
public final class BoundedChatStreamExecutor implements ChatStreamExecutor, AutoCloseable {
    private final ThreadPoolExecutor workers;
    private final ScheduledThreadPoolExecutor scheduler;

    /** 유한 실행 자원 생성
     * @param count 최대 worker 수
     */
    public BoundedChatStreamExecutor(int count) {
        workers = new ThreadPoolExecutor(count, count, 0, TimeUnit.SECONDS, new SynchronousQueue<>(),
                Thread.ofPlatform().name("ai-chat-worker-", 0).factory(), new ThreadPoolExecutor.AbortPolicy());
        scheduler = new ScheduledThreadPoolExecutor(1, Thread.ofPlatform().name("ai-chat-deadline-", 0).factory());
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    }

    @Override
    public Future<?> submit(Runnable task) { return workers.submit(task); }

    @Override
    public Future<?> schedule(Runnable task, Duration delay) {
        return scheduler.schedule(task, delay.toNanos(), TimeUnit.NANOSECONDS);
    }

    /** scheduler와 실행 중 worker에 종료 요청 */
    @Override
    public void close() {
        scheduler.shutdownNow();
        workers.shutdownNow();
    }
}
