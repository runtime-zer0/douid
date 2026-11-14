package kr.douid.brand.ai.infrastructure.guard;
import static org.assertj.core.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import kr.douid.brand.ai.application.AiChatException;
import kr.douid.brand.ai.application.port.ChatRequestGuard;
import kr.douid.brand.ai.testsupport.Policies;

class InMemoryChatRequestGuardTest {
    @Test void 요청시도와_거부_점유를_비식별_지표로_집계() {
        var metrics=new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        var guard=new InMemoryChatRequestGuard(Policies.defaults(),()->0L,metrics);
        var first=guard.acquire();var second=guard.acquire();
        assertThatThrownBy(guard::acquire).isInstanceOf(AiChatException.class);
        assertThat(metrics.get("ai.chat.active").gauge().value()).isEqualTo(2);
        first.close();first.close();second.close();
        assertThat(metrics.get("ai.chat.active").gauge().value()).isZero();
        for(int i=0;i<10;i++)guard.attempt();
        assertThatThrownBy(guard::attempt).isInstanceOf(AiChatException.class);
        assertThat(metrics.get("ai.chat.attempts").counter().count()).isEqualTo(11);
        assertThat(metrics.get("ai.chat.rejected").tag("reason","rate").counter().count()).isEqualTo(1);
        assertThat(metrics.get("ai.chat.rejected").tag("reason","concurrency").counter().count()).isEqualTo(1);
    }
    @Test void 창_한도와_다음창_교체_RetryAfter() {
        var time = new AtomicLong();
        var guard = new InMemoryChatRequestGuard(Policies.defaults(), time::get);
        for (int i=0;i<10;i++) guard.attempt();
        assertThatThrownBy(guard::attempt).isInstanceOfSatisfying(AiChatException.class,
                e -> { assertThat(e.getCode()).isEqualTo("AI_RATE_LIMITED");
                    assertThat(e.getRetryAfterSeconds()).isEqualTo(60); });
        time.set(TimeUnit.SECONDS.toNanos(59)+1);
        assertThatThrownBy(guard::attempt).isInstanceOfSatisfying(AiChatException.class,
                e -> assertThat(e.getRetryAfterSeconds()).isEqualTo(1));
        time.set(TimeUnit.SECONDS.toNanos(60));
        guard.attempt();
    }
    @Test void 동시_점유_2개와_중복_반환() {
        var guard = new InMemoryChatRequestGuard(Policies.defaults(), System::nanoTime);
        List<ChatRequestGuard.Lease> leases = new ArrayList<>();
        for(int i=0;i<2;i++) leases.add(guard.acquire());
        assertThatThrownBy(guard::acquire).isInstanceOf(AiChatException.class);
        leases.getFirst().close(); leases.getFirst().close();
        var replacement=guard.acquire();
        assertThatThrownBy(guard::acquire).isInstanceOf(AiChatException.class);
        replacement.close(); leases.forEach(ChatRequestGuard.Lease::close);
    }
    @Test void 경쟁하는_시도도_10개만_허용() throws Exception {
        var guard = new InMemoryChatRequestGuard(Policies.defaults(), () -> 0L);
        var accepted = new AtomicInteger();
        try(var threads=Executors.newFixedThreadPool(12)) {
            List<Future<?>> calls = new ArrayList<>();
            for(int i=0;i<100;i++) calls.add(threads.submit(() -> {
                try { guard.attempt(); accepted.incrementAndGet(); } catch(AiChatException ignored) {}
            }));
            for(var call:calls) call.get(3, TimeUnit.SECONDS);
        }
        assertThat(accepted.get()).isEqualTo(10);
    }
}
