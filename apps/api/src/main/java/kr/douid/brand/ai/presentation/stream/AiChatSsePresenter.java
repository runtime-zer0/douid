package kr.douid.brand.ai.presentation.stream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.BooleanSupplier;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import kr.douid.brand.ai.application.*;
import kr.douid.brand.ai.application.port.StreamSink;

/** 단일 writer에서 명명 SSE 이벤트와 terminal을 전송하는 adapter */
public final class AiChatSsePresenter implements StreamSink {
    private final AiChatPolicy policy;
    private final SseEmitter emitter;
    private boolean started;
    private boolean terminal;
    private volatile boolean disconnected;
    private long outputBytes;
    private int deltas;
    private BooleanSupplier stopped = () -> false;

    /** 전송 adapter 생성
     * @param policy 출력·시간 제한 정책
     */
    public AiChatSsePresenter(AiChatPolicy policy) {
        this(policy, new SseEmitter(policy.emitterTimeout().toMillis()));
    }
    AiChatSsePresenter(AiChatPolicy policy, SseEmitter emitter) {
        this.policy = policy; this.emitter = emitter;
    }

    /** 실행 전 container callback 연결
     * @param execution 실행 소유권
     */
    public void attach(AiChatExecution execution) {
        stopped = execution::stopped;
        emitter.onTimeout(() -> { disconnected = true; execution.transportFailed(); });
        emitter.onError(error -> { disconnected = true; execution.transportFailed(); });
        emitter.onCompletion(() -> { disconnected = true; execution.transportCompleted(); });
    }

    /** HTTP 응답 emitter 조회
     * @return callback이 연결된 emitter
     */
    public SseEmitter emitter() { return emitter; }

    /** 요청 수락 이벤트 전송
     * @param requestId 임시 요청 식별자
     */
    @Override
    public synchronized void started(String requestId) {
        if (terminal || disconnected || started) throw new Disconnected();
        send("started", new Started(requestId)); started = true;
    }

    /** 예산 내 텍스트 조각 전송
     * @param text 원형을 유지할 텍스트
     */
    @Override
    public synchronized void delta(String text) {
        if (terminal || disconnected || !started) throw new Disconnected();
        if (stopped.getAsBoolean()) return;
        if (text == null) throw new IllegalArgumentException("Invalid source delta");
        long bytes = text.getBytes(StandardCharsets.UTF_8).length;
        if (deltas >= policy.maxDeltaEvents() || bytes > policy.maxOutputBytes() - outputBytes) {
            throw new LimitExceeded();
        }
        send("message_delta", new Delta(text)); outputBytes += bytes; deltas++;
    }

    /** 정상 terminal 한 번 전송 후 연결 종료 요청 */
    @Override
    public synchronized void completed() {
        if (terminal || disconnected) return;
        terminal = true;
        send("completed", new Completed("success")); emitter.complete();
    }

    /** 초기화 후 실패를 HTTP 또는 SSE 경계로 변환
     * @param reason 첫 종료 사유
     */
    @Override
    public synchronized void failed(StopReason reason) {
        if (terminal || disconnected) return;
        terminal = true;
        if (!started) {
            emitter.completeWithError(reason == StopReason.TIMED_OUT
                    ? new AiChatException.Timeout() : new AiChatException.Unavailable());
            return;
        }
        Error error = switch (reason) {
            case TIMED_OUT -> new Error("STREAM_TIMEOUT", "응답 시간이 초과되었습니다.", true);
            case LIMIT_EXCEEDED -> new Error("STREAM_LIMIT_EXCEEDED", "응답이 허용된 범위를 초과했습니다.", false);
            default -> new Error("STREAM_FAILED", "응답을 처리하지 못했습니다.", false);
        };
        send("error", error); emitter.complete();
    }

    private void send(String event, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(event).data(payload, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            disconnected = true;
            // IOException 후의 연결 종료는 container 소유, complete 재호출 금지
            throw new Disconnected();
        }
    }
    public record Started(String requestId) {}
    public record Delta(String text) {}
    public record Completed(String status) {}
    public record Error(String code, String message, boolean retryable) {}
}
