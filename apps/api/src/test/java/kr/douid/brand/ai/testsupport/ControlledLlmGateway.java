package kr.douid.brand.ai.testsupport;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;
import kr.douid.brand.ai.application.AiChatCommand;
import kr.douid.brand.ai.application.port.LlmGateway;

/** latch로 정상·오류·취소 시점을 제어하는 테스트 전용 공급자 */
public final class ControlledLlmGateway implements LlmGateway {
    public interface Behavior {
        /** 제어된 소스 동작
         * @param stream 실행 중 소스
         * @param delta 순차 출력
         * @throws Exception 테스트용 실행 오류
         */
        void run(ControlledStream stream, Consumer<String> delta) throws Exception;
    }
    public volatile Behavior behavior = (stream, delta) -> {};
    public final AtomicInteger prepared = new AtomicInteger();
    public volatile AiChatCommand command;
    public volatile ControlledStream last;
    public final java.util.Queue<ControlledStream> streams = new java.util.concurrent.ConcurrentLinkedQueue<>();
    /** 요청별 독립 실행 준비
     * @param command 비신뢰 입력
     * @return 제어 가능한 소스
     */
    @Override public LlmStream prepare(AiChatCommand command) {
        this.command=command; prepared.incrementAndGet();
        last=new ControlledStream(behavior); streams.add(last); return last;
    }
    public static final class ControlledStream implements LlmStream {
        private final Behavior behavior;
        public final CountDownLatch entered=new CountDownLatch(1);
        public final CountDownLatch release=new CountDownLatch(1);
        public final CountDownLatch closed=new CountDownLatch(1);
        public final CountDownLatch cancelRequested=new CountDownLatch(1);
        public final AtomicBoolean cancelled=new AtomicBoolean();
        private final AtomicBoolean ran=new AtomicBoolean();
        ControlledStream(Behavior behavior) {this.behavior=behavior;}
        /** 한 번만 실행되는 latch 제어 소스
         * @param delta 순차 출력
         * @throws Exception 제어 동작 오류
         */
        @Override public void run(Consumer<String> delta) throws Exception {
            if(!ran.compareAndSet(false,true)) throw new IllegalStateException();
            entered.countDown();behavior.run(this,delta);
        }
        /** 취소 요청 관찰 */
        @Override public void cancel() {cancelled.set(true);cancelRequested.countDown();}
        /** 실제 실행 종료 관찰 */
        @Override public void close() {closed.countDown();}
        /** 최대 5초의 완료 보류
         * @throws InterruptedException 취소에 의한 중단
         */
        public void awaitRelease() throws InterruptedException {
            if(!release.await(5,TimeUnit.SECONDS)) throw new IllegalStateException("test source deadline");
        }
    }
}
