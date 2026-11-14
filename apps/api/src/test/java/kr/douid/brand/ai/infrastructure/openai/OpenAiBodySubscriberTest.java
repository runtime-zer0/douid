package kr.douid.brand.ai.infrastructure.openai;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.Test;

class OpenAiBodySubscriberTest {
    @Test void 취소_후_늦은_subscription도_취소() {
        var subscriber = new OpenAiBodySubscriber(10);
        subscriber.cancel(); subscriber.cancel();
        var subscription = mock(Flow.Subscription.class);
        subscriber.onSubscribe(subscription);
        verify(subscription, atLeastOnce()).cancel();
    }
    @Test void 정확한_본문한도와_초과의_안전한_종료() throws Exception {
        for (int count : new int[]{10, 11}) {
            var subscriber = new OpenAiBodySubscriber(10);
            var subscription = mock(Flow.Subscription.class);
            subscriber.onSubscribe(subscription);
            var body = subscriber.getBody().toCompletableFuture().get();
            subscriber.onNext(List.of(ByteBuffer.wrap(new byte[count])));
            subscriber.onComplete();
            if (count == 10) assertThat(body.readAllBytes()).hasSize(10);
            else {
                assertThatThrownBy(body::readAllBytes).isInstanceOf(java.io.IOException.class);
                assertThat(subscriber.exceeded()).isTrue(); verify(subscription, atLeastOnce()).cancel();
            }
            body.close();
        }
    }
    @Test void 중복_subscription을_추가로_요청하지않음() {
        var subscriber = new OpenAiBodySubscriber(10);
        subscriber.onSubscribe(mock(Flow.Subscription.class));
        var duplicate = mock(Flow.Subscription.class);
        subscriber.onSubscribe(duplicate);
        verify(duplicate).cancel(); verify(duplicate, never()).request(anyLong());
        subscriber.cancel();
    }
}
