package kr.douid.brand.ai.presentation.stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import kr.douid.brand.ai.application.AiChatPolicy;
import kr.douid.brand.ai.application.port.StreamSink;
import kr.douid.brand.ai.application.port.StreamSink.StopReason;
import kr.douid.brand.ai.testsupport.Policies;

class AiChatSsePresenterTest {
    @Test void terminal_중복과_이후_Delta_차단() throws Exception {
        var emitter=mock(SseEmitter.class);var presenter=new AiChatSsePresenter(Policies.defaults(),emitter);
        presenter.started("id");presenter.delta("a");presenter.completed();presenter.completed();
        presenter.failed(StopReason.FAILED);
        assertThatThrownBy(()->presenter.delta("late")).isInstanceOf(StreamSink.Disconnected.class);
        verify(emitter,times(3)).send(any(SseEmitter.SseEventBuilder.class));verify(emitter,times(1)).complete();
    }
    @Test void 모든_단계의_쓰기실패후_complete_호출하지_않음() throws Exception {
        for(int failureAt:new int[]{1,2,3}) {
            var emitter=mock(SseEmitter.class);var count=new java.util.concurrent.atomic.AtomicInteger();
            doAnswer(invocation->{if(count.incrementAndGet()==failureAt)throw new IOException("secret");return null;})
                    .when(emitter).send(any(SseEmitter.SseEventBuilder.class));
            var presenter=new AiChatSsePresenter(Policies.defaults(),emitter);
            assertThatThrownBy(()->{presenter.started("id");presenter.delta("a");presenter.completed();})
                    .isInstanceOf(StreamSink.Disconnected.class);
            presenter.failed(StopReason.FAILED);
            verify(emitter,never()).complete();verify(emitter,never()).completeWithError(any());
        }
    }
    @Test void 출력_byte와_Delta_한도는_초과조각을_보내기전_차단() throws Exception {
        var p=Policies.defaults();
        var limited=new AiChatPolicy(p.maxRequestBytes(),p.maxMessageCodePoints(),p.maxHistoryMessages(),
                p.maxHistoryMessageCodePoints(),p.maxContextCodePoints(),p.rateRequests(),p.rateWindow(),
                p.maxConcurrentStreams(),p.executionTimeout(),p.emitterTimeout(),3,2);
        var emitter=mock(SseEmitter.class);var presenter=new AiChatSsePresenter(limited,emitter);
        presenter.started("id");presenter.delta("한");
        assertThatThrownBy(()->presenter.delta("a")).isInstanceOf(StreamSink.LimitExceeded.class);
        presenter.delta("");
        assertThatThrownBy(()->presenter.delta("")).isInstanceOf(StreamSink.LimitExceeded.class);
        presenter.failed(StopReason.LIMIT_EXCEEDED);
        verify(emitter,times(4)).send(any(SseEmitter.SseEventBuilder.class));verify(emitter).complete();
    }
    @Test void 시작전_timeout은_SSE가_아닌_504_예외_dispatch() {
        var emitter=mock(SseEmitter.class);var presenter=new AiChatSsePresenter(Policies.defaults(),emitter);
        presenter.failed(StopReason.TIMED_OUT);presenter.failed(StopReason.FAILED);
        verify(emitter).completeWithError(isA(kr.douid.brand.ai.application.AiChatException.Timeout.class));
        verify(emitter,never()).complete();
    }
    @Test void deadline과_Delta_경합은_전송단절로_오인하지_않고_timeout_error_유지() throws Exception {
        var emitter=mock(SseEmitter.class);var execution=mock(kr.douid.brand.ai.application.AiChatExecution.class);
        var presenter=new AiChatSsePresenter(Policies.defaults(),emitter);presenter.attach(execution);
        presenter.started("id");when(execution.stopped()).thenReturn(true);
        presenter.delta("late");presenter.failed(StopReason.TIMED_OUT);
        verify(emitter,times(2)).send(any(SseEmitter.SseEventBuilder.class));verify(emitter).complete();
    }
}
