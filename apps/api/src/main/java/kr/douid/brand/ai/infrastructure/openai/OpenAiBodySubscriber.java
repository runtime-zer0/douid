package kr.douid.brand.ai.infrastructure.openai;

import java.io.InputStream;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** JDK 유한 InputStream 위임과 비차단 취소·수신 시각 추적 */
final class OpenAiBodySubscriber implements HttpResponse.BodySubscriber<InputStream> {
    private final HttpResponse.BodySubscriber<InputStream> delegate;
    private final long maxBytes;
    private final AtomicReference<Flow.Subscription> subscription = new AtomicReference<>();
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private long received;
    private volatile boolean exceeded;
    private volatile long lastReceivedAt = System.nanoTime();

    OpenAiBodySubscriber(long maxBytes) {
        this.maxBytes = maxBytes;
        delegate = HttpResponse.BodySubscribers.limiting(HttpResponse.BodySubscribers.ofInputStream(), maxBytes);
    }
    long lastReceivedAt() { return lastReceivedAt; }
    boolean exceeded() { return exceeded; }
    void cancel() {
        cancelled.set(true);
        var current = subscription.get();
        if (current != null) current.cancel();
    }
    @Override public CompletionStage<InputStream> getBody() { return delegate.getBody(); }
    /** 늦은 subscription에도 기존 취소 신호 적용
     * @param incoming JDK 수신 제어 핸들
     */
    @Override public void onSubscribe(Flow.Subscription incoming) {
        if (!subscription.compareAndSet(null, incoming)) { incoming.cancel(); return; }
        delegate.onSubscribe(incoming);
        if (cancelled.get()) incoming.cancel();
    }
    /** 본문을 복제하지 않는 수신 시각·한도 추적
     * @param buffers JDK 수신 byte 묶음
     */
    @Override public void onNext(List<ByteBuffer> buffers) {
        lastReceivedAt = System.nanoTime();
        for (var buffer : buffers) received += buffer.remaining();
        if (received > maxBytes) exceeded = true;
        delegate.onNext(buffers);
    }
    @Override public void onError(Throwable error) { delegate.onError(error); }
    @Override public void onComplete() { delegate.onComplete(); }
}
