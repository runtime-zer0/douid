package kr.douid.brand.ai.application;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import kr.douid.brand.ai.application.port.*;
import kr.douid.brand.ai.application.port.StreamSink.StopReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 한 요청의 단일 실행·취소·실제 종료 및 연결 점유 소유권 */
public final class AiChatExecution {
    private static final Logger log = LoggerFactory.getLogger(AiChatExecution.class);
    private final AiChatUseCase.Prepared prepared;
    private final StreamSink sink;
    private final ChatStreamExecutor executor;
    private final AiChatPolicy policy;
    private final String requestId = UUID.randomUUID().toString();
    private final long acceptedAt = System.nanoTime();
    private final CountDownLatch activation = new CountDownLatch(1);
    private final AtomicReference<StopReason> reason = new AtomicReference<>();
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean transportEnded = new AtomicBoolean();
    private final AtomicBoolean workerExited = new AtomicBoolean();
    private final AtomicBoolean returned = new AtomicBoolean();
    private final AtomicBoolean sourceClosed = new AtomicBoolean();
    private final AtomicBoolean deliveryFailed = new AtomicBoolean();
    private volatile boolean transportFailed;
    private volatile Future<?> deadline;
    private volatile Future<?> future;
    private volatile Thread worker;

    AiChatExecution(AiChatUseCase.Prepared prepared, StreamSink sink, ChatStreamExecutor executor, AiChatPolicy policy) {
        this.prepared = prepared; this.sink = sink; this.executor = executor; this.policy = policy;
    }

    /** 초기화 gate에서 대기할 실행 제출, 거부 시 미실행 자원 정리 */
    public void start() {
        if (!started.compareAndSet(false, true)) throw new IllegalStateException("Stream already submitted");
        try {
            deadline = executor.schedule(() -> stop(StopReason.TIMED_OUT), policy.executionTimeout());
            future = executor.submit(this::run);
        } catch (RejectedExecutionException e) {
            abortBeforeStart();
            throw AiChatException.limited(false, 1);
        }
    }

    /** 종료 사유 선택 여부
     * @return 새 Delta 금지 여부
     */
    public boolean stopped() { return reason.get() != null || transportFailed; }

    /** MVC emitter 초기화 이후 실행 gate 개방 */
    public void activate() { activation.countDown(); }

    /** 초기화 실패 시 전송 없이 실행 폐기 */
    public void abortBeforeStart() {
        transportFailed = true;
        stop(StopReason.CANCELLED);
        transportEnded.set(true);
        activation.countDown();
        if (!started.get() || future == null) {
            closeSource(); workerExited.set(true);
        }
        cleanup();
    }

    /** 컨테이너의 timeout/error 신호, callback에서 네트워크 쓰기 금지 */
    public void transportFailed() {
        transportFailed = true;
        stop(StopReason.DISCONNECTED);
        activation.countDown();
    }

    /** 최종 연결 종료 확인, 실제 worker 종료와 함께 점유 반환 */
    public void transportCompleted() {
        transportEnded.set(true);
        transportFailed();
        cancelDeadline();
        cleanup();
    }

    private void run() {
        worker = Thread.currentThread();
        try {
            activation.await();
            if (transportFailed || transportEnded.get()) return;
            if (System.nanoTime() - acceptedAt >= policy.executionTimeout().toNanos()) stop(StopReason.TIMED_OUT);
            if (reason.get() == null) {
                sink.started(requestId);
                if (reason.get() == null) prepared.source().run(text -> {
                    if (reason.get() != null) throw new Stopped();
                    sink.delta(text);
                });
                reason.compareAndSet(null, StopReason.SUCCEEDED);
            }
        } catch (StreamSink.LimitExceeded e) {
            stop(StopReason.LIMIT_EXCEEDED);
        } catch (StreamSink.Disconnected e) {
            deliveryFailed.set(true); transportFailed = true; stop(StopReason.DISCONNECTED);
        } catch (LlmIntegrationException e) {
            stop(switch (e.kind()) {
                case CONNECT_TIMEOUT, HEADER_TIMEOUT, COUNT_TIMEOUT, READ_IDLE_TIMEOUT -> StopReason.TIMED_OUT;
                case INPUT_BUDGET_EXCEEDED, PROVIDER_RESOURCE_LIMIT -> StopReason.LIMIT_EXCEEDED;
                default -> StopReason.FAILED;
            });
        } catch (Exception e) {
            stop(StopReason.FAILED);
        } finally {
            // interrupt 상태를 해제해 선택된 terminal의 마지막 전송 시도 허용
            Thread.interrupted();
            try {
                if (!transportFailed && !transportEnded.get()) {
                    if (reason.get() == StopReason.SUCCEEDED) sink.completed();
                    else if (reason.get() != null) sink.failed(reason.get());
                }
            } catch (RuntimeException e) {
                deliveryFailed.set(true);
            } finally {
                try { closeSource(); }
                finally {
                    worker = null; workerExited.set(true); cancelDeadline(); cleanup();
                    log.info("AI stream requestId={} reason={} delivered={} elapsedMs={}", requestId,
                            reason.get(), !deliveryFailed.get(), (System.nanoTime() - acceptedAt) / 1_000_000);
                }
            }
        }
    }

    private void stop(StopReason why) {
        if (reason.compareAndSet(null, why) && why != StopReason.SUCCEEDED) {
            try { prepared.source().cancel(); } catch (RuntimeException ignored) { }
            Thread executing = worker;
            // gate 초기화 전 timeout은 pending으로 유지하고 활성화 뒤 504로 종료
            if (executing != null && activation.getCount() == 0) {
                var handle = future;
                if (handle != null) handle.cancel(true);
                else executing.interrupt();
            }
        }
    }

    private void closeSource() {
        if (sourceClosed.compareAndSet(false, true)) {
            try { prepared.source().close(); } catch (RuntimeException ignored) { }
        }
    }
    private void cancelDeadline() { var timer = deadline; if (timer != null) timer.cancel(false); }
    private void cleanup() {
        if (workerExited.get() && transportEnded.get() && returned.compareAndSet(false, true)) prepared.lease().close();
    }
    private static final class Stopped extends RuntimeException {}
}
