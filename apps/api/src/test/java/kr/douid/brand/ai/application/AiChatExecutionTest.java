package kr.douid.brand.ai.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import kr.douid.brand.ai.application.port.*;
import kr.douid.brand.ai.application.port.StreamSink.StopReason;
import kr.douid.brand.ai.infrastructure.stream.BoundedChatStreamExecutor;
import kr.douid.brand.ai.testsupport.*;

class AiChatExecutionTest {
    final ControlledLlmGateway gateway=new ControlledLlmGateway();
    final ChatRequestGuard guard=mock(ChatRequestGuard.class);
    final ChatRequestGuard.Lease lease=mock(ChatRequestGuard.Lease.class);
    final StreamSink sink=mock(StreamSink.class);
    final BoundedChatStreamExecutor executor=new BoundedChatStreamExecutor(1);
    final AiChatPolicy defaults=Policies.defaults();
    @BeforeEach void setup(){when(guard.acquire()).thenReturn(lease);}
    @AfterEach void close(){if(gateway.last!=null)gateway.last.release.countDown();executor.close();}
    AiChatExecution start(AiChatPolicy policy) {
        var useCase=new AiChatUseCase(gateway,guard,policy,executor);
        return useCase.start(useCase.prepare(new AiChatCommand("hi",List.of())),sink,e->{});
    }
    @Test void worker_종료후에도_최종_연결종료까지_점유_유지() throws Exception {
        var execution=start(defaults);execution.activate();
        assertThat(gateway.last.closed.await(3,TimeUnit.SECONDS)).isTrue();
        verify(lease,never()).close();
        execution.transportCompleted();execution.transportCompleted();
        verify(lease,times(1)).close();verify(sink).completed();
    }
    @Test void 취소무시_worker는_연결종료후에도_실제_exit까지_점유() throws Exception {
        gateway.behavior=(stream,delta)->{while(stream.release.getCount()>0) {
            try {stream.release.await();}catch(InterruptedException ignored){}
        }};
        var execution=start(defaults);execution.activate();
        assertThat(gateway.last.entered.await(3,TimeUnit.SECONDS)).isTrue();
        execution.transportCompleted();
        verify(lease,never()).close();assertThat(gateway.last.cancelled.get()).isTrue();
        gateway.last.release.countDown();
        verify(lease,timeout(3000).times(1)).close();verify(sink,never()).completed();
    }
    @Test void 초기화전_deadline은_pending이며_소스없이_timeout_전달() throws Exception {
        var p=defaults;
        var policy=new AiChatPolicy(p.maxRequestBytes(),p.maxMessageCodePoints(),p.maxHistoryMessages(),
                p.maxHistoryMessageCodePoints(),p.maxContextCodePoints(),p.rateRequests(),p.rateWindow(),1,
                Duration.ofMillis(25),Duration.ofSeconds(1),p.maxOutputBytes(),p.maxDeltaEvents());
        var execution=start(policy);
        org.mockito.Mockito.verify(sink,after(100).never()).started(anyString());
        assertThat(gateway.last.cancelled.get()).isTrue();
        execution.activate();verify(sink,timeout(3000)).failed(StopReason.TIMED_OUT);
        assertThat(gateway.last.entered.getCount()).isEqualTo(1);
        execution.transportCompleted();verify(lease,timeout(3000)).close();
    }
    @Test void 부분_Delta_오류는_실패만_전달하고_source_한번_정리() throws Exception {
        gateway.behavior=(stream,delta)->{delta.accept("partial");throw new RuntimeException("secret-canary");};
        var execution=start(defaults);execution.activate();
        assertThat(gateway.last.closed.await(3,TimeUnit.SECONDS)).isTrue();
        verify(sink).delta("partial");verify(sink).failed(StopReason.FAILED);verify(sink,never()).completed();
        execution.transportCompleted();verify(lease).close();
    }
    @Test void callback_등록중_취소와_초기화실패는_실행없이_정리() {
        var useCase=new AiChatUseCase(gateway,guard,defaults,executor);
        var prepared=useCase.prepare(new AiChatCommand("hi",List.of()));
        assertThatThrownBy(()->useCase.start(prepared,sink,e->{throw new IllegalStateException();}))
                .isInstanceOf(IllegalStateException.class);
        verify(lease).close();assertThat(gateway.last.closed.getCount()).isZero();
    }
    @RepeatedTest(10)
    void 완료와_중복_연결종료_경합은_terminal과_점유반환_최대한번() throws Exception {
        gateway.behavior=(stream,delta)->{delta.accept("before");stream.awaitRelease();};
        var execution=start(defaults);execution.activate();
        assertThat(gateway.last.entered.await(3,TimeUnit.SECONDS)).isTrue();
        var barrier=new CyclicBarrier(3);
        try(var racers=Executors.newFixedThreadPool(3)) {
            var success=racers.submit(()->{barrier.await(3,TimeUnit.SECONDS);gateway.last.release.countDown();return null;});
            var error=racers.submit(()->{barrier.await(3,TimeUnit.SECONDS);execution.transportFailed();return null;});
            var completion=racers.submit(()->{barrier.await(3,TimeUnit.SECONDS);execution.transportCompleted();return null;});
            success.get(3,TimeUnit.SECONDS);error.get(3,TimeUnit.SECONDS);completion.get(3,TimeUnit.SECONDS);
        }
        verify(lease,timeout(3000).times(1)).close();
        execution.transportCompleted();
        long terminals=mockingDetails(sink).getInvocations().stream()
                .filter(invocation->java.util.Set.of("completed","failed").contains(invocation.getMethod().getName())).count();
        assertThat(terminals).isLessThanOrEqualTo(1);
        verify(lease,times(1)).close();
    }
    @Test void 초기화전_연결종료와_늦은_activation은_source_미실행() throws Exception {
        var execution=start(defaults);execution.transportCompleted();execution.activate();execution.activate();
        assertThat(gateway.last.closed.await(3,TimeUnit.SECONDS)).isTrue();
        assertThat(gateway.last.entered.getCount()).isEqualTo(1);
        verify(lease,timeout(3000).times(1)).close();verifyNoInteractions(sink);
    }
    @Test void 실행제출_거부는_시작전_429와_미실행_자원정리() {
        var rejecting=mock(ChatStreamExecutor.class);
        when(rejecting.schedule(any(),any())).thenReturn(mock(Future.class));
        when(rejecting.submit(any())).thenThrow(new RejectedExecutionException());
        var useCase=new AiChatUseCase(gateway,guard,defaults,rejecting);
        assertThatThrownBy(()->useCase.start(useCase.prepare(new AiChatCommand("hi",List.of())),sink,e->{}))
                .isInstanceOfSatisfying(AiChatException.class,e->assertThat(e.getCode()).isEqualTo("AI_BUSY"));
        assertThat(gateway.last.closed.getCount()).isZero();verify(lease,times(1)).close();
    }
}
