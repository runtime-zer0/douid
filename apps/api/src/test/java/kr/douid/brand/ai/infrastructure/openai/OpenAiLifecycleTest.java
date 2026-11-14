package kr.douid.brand.ai.infrastructure.openai;

import static org.assertj.core.api.Assertions.*;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import kr.douid.brand.ai.application.LlmIntegrationException;
import kr.douid.brand.ai.testsupport.OpenAiStubServer;
import static kr.douid.brand.ai.application.LlmIntegrationException.Kind.*;

@Timeout(10)
class OpenAiLifecycleTest extends OpenAiTestSupport {
    @Test void 느린_소비자도_관측유휴_기한에_포함() throws Exception {
        properties.setReadIdleTimeout(Duration.ofMillis(100));
        try(var source=source()) {
            assertThatThrownBy(()->source.run(s->{
                try {new CountDownLatch(1).await();}catch(InterruptedException e){throw new IllegalStateException();}
            })).isInstanceOfSatisfying(LlmIntegrationException.class,e->assertThat(e.kind()).isEqualTo(READ_IDLE_TIMEOUT));
        }finally{Thread.interrupted();}
    }
    @Test void 정상종료_뒤_watchdog와_늦은_callback은_요청을_다시_취소하지않음() throws Exception {
        var scheduled=new java.util.ArrayList<Future<?>>();
        var callbacks=new java.util.ArrayList<Runnable>();
        var observed=org.mockito.Mockito.spy(executor);
        org.mockito.Mockito.doAnswer(call->{
            callbacks.add(call.getArgument(0));
            Future<?> future=(Future<?>)call.callRealMethod();scheduled.add(future);return future;
        }).when(observed).schedule(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());
        var adapter=OpenAiResponsesGateway.forLoopbackTest(properties,client,observed,metrics,stub.endpoint());
        try(var source=adapter.prepare(new kr.douid.brand.ai.application.AiChatCommand("hello",java.util.List.of()))) {
            source.run(s->{});
            assertThat(scheduled).hasSize(3).allMatch(Future::isCancelled);
            callbacks.forEach(Runnable::run);
            assertThat(Thread.currentThread().isInterrupted()).isFalse();
        }
    }
    @Test void 실행전_취소와_중복close는_외부호출하지않음() throws Exception {
        var source = source(); source.cancel(); source.cancel(); source.close(); source.close();
        assertThatThrownBy(() -> source.run(s -> {})).isInstanceOfSatisfying(LlmIntegrationException.class,
                e -> assertThat(e.kind()).isEqualTo(CLIENT_CANCELLED));
        assertThat(stub.countRequests).isEmpty(); assertThat(stub.generationRequests).isEmpty();
    }
    @Test void Count본문_전체기한이_지나면_생성하지않음() throws Exception {
        properties.setCountTimeout(Duration.ofMillis(100));
        var release = new CountDownLatch(1);
        stub.count = e -> {
            e.getResponseHeaders().set("Content-Type", "application/json"); e.sendResponseHeaders(200, 0);
            e.getResponseBody().write('{'); e.getResponseBody().flush(); release.await();
        };
        try (var source = source()) {
            assertThatThrownBy(() -> source.run(s -> {})).isInstanceOfSatisfying(LlmIntegrationException.class,
                    e -> assertThat(e.kind()).isEqualTo(COUNT_TIMEOUT));
        } finally { Thread.interrupted(); release.countDown(); }
        assertThat(stub.generationRequests).isEmpty();
    }
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void 생성_헤더와_수신유휴_timeout을_구분(boolean headers) throws Exception {
        properties.setResponseHeaderTimeout(Duration.ofMillis(100));
        properties.setReadIdleTimeout(Duration.ofMillis(100));
        var release = new CountDownLatch(1);
        stub.generation = e -> {
            if (!headers) {
                e.getResponseHeaders().set("Content-Type", "text/event-stream"); e.sendResponseHeaders(200, 0);
                e.getResponseBody().write(": start\n\n".getBytes()); e.getResponseBody().flush();
            }
            release.await();
        };
        try (var source = source()) {
            assertThatThrownBy(() -> source.run(s -> {})).isInstanceOfSatisfying(LlmIntegrationException.class,
                    e -> assertThat(e.kind()).isEqualTo(headers ? HEADER_TIMEOUT : READ_IDLE_TIMEOUT));
        } finally { Thread.interrupted(); release.countDown(); }
    }
    @Test void 읽기중_취소는_worker를_해제하며_공유client는_유지() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        stub.generation = e -> {
            e.getResponseHeaders().set("Content-Type", "text/event-stream"); e.sendResponseHeaders(200, 0);
            e.getResponseBody().write(": start\n\n".getBytes()); e.getResponseBody().flush(); entered.countDown(); release.await();
        };
        try (var source = source(); var threads = Executors.newSingleThreadExecutor()) {
            var result = threads.submit(() -> { source.run(s -> {}); return null; });
            assertThat(entered.await(3, TimeUnit.SECONDS)).isTrue();
            source.cancel(); source.cancel();
            assertThatThrownBy(() -> result.get(3, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class);
            assertThat(client.isTerminated()).isFalse();
        } finally { release.countDown(); }
    }
    @Test void Count후_취소는_생성을_시작하지않음() throws Exception {
        AtomicReference<kr.douid.brand.ai.application.port.LlmGateway.LlmStream> ref = new AtomicReference<>();
        stub.count = e -> {
            ref.get().cancel();
            OpenAiStubServer.reply(e, 200, "application/json", "{\"object\":\"response.input_tokens\",\"input_tokens\":1}");
        };
        try (var source = source()) {
            ref.set(source);
            assertThatThrownBy(() -> source.run(s -> {})).isInstanceOf(LlmIntegrationException.class);
        } finally { Thread.interrupted(); }
        assertThat(stub.generationRequests).isEmpty();
    }
}
