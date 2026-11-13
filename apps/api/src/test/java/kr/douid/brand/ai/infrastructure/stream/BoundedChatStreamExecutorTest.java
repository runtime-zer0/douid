package kr.douid.brand.ai.infrastructure.stream;
import static org.assertj.core.api.Assertions.*;
import java.time.Duration;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class BoundedChatStreamExecutorTest {
    @Test void 실행중_취소는_실제_종료가_아니며_대기열없이_거부() throws Exception {
        var started=new CountDownLatch(1);var release=new CountDownLatch(1);
        try(var executor=new BoundedChatStreamExecutor(1)) {
            try {
                var future=executor.submit(() -> {
                    started.countDown();
                    while(release.getCount()>0) {
                        try { release.await(); } catch(InterruptedException ignored) {}
                    }
                });
                assertThat(started.await(3,TimeUnit.SECONDS)).isTrue();
                future.cancel(true);
                assertThat(future.isDone()).isTrue();
                assertThatThrownBy(() -> executor.submit(() -> {})).isInstanceOf(RejectedExecutionException.class);
            } finally { release.countDown(); }
        }
    }
    @Test void 예약_취소와_종료후_제출_거부() {
        var executor=new BoundedChatStreamExecutor(1);
        var timer=executor.schedule(() -> {throw new AssertionError();},Duration.ofHours(1));
        assertThat(timer.cancel(false)).isTrue();
        executor.close();
        assertThatThrownBy(() -> executor.submit(() -> {})).isInstanceOf(RejectedExecutionException.class);
        assertThatThrownBy(() -> executor.schedule(() -> {},Duration.ofSeconds(1)))
                .isInstanceOf(RejectedExecutionException.class);
    }
}
