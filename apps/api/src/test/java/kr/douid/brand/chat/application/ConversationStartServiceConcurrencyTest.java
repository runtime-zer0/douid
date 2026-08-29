package kr.douid.brand.chat.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import kr.douid.brand.client.application.ClientIdentityProvisioningService;
import kr.douid.brand.shared.testsupport.PostgresIntegrationTest;

/**
 * 동일 상담 주체에서 거의 동시에 여러 상담 시작 요청이 발생해도 활성 Conversation이 하나만
 * 존재하는지 검증(FR-026) — partial unique index 위반 시 재조회 흐름이 정상 동작하는지 확인한다.
 */
@SpringBootTest
class ConversationStartServiceConcurrencyTest extends PostgresIntegrationTest {

    @Autowired
    private ConversationStartService conversationStartService;

    @Autowired
    private ClientIdentityProvisioningService clientIdentityProvisioningService;

    @Test
    void start_동시요청_활성상담최대1개() throws InterruptedException {
        Long clientIdentityId = clientIdentityProvisioningService.provision().clientIdentityId();

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<UUID> conversationPublicIds = new CopyOnWriteArrayList<>();

        IntStream.range(0, threadCount).forEach(i -> executor.submit(() -> {
            try {
                startLatch.await();
                var result = conversationStartService.start(Optional.of(clientIdentityId));
                conversationPublicIds.add(result.conversationPublicId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        }));

        startLatch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(conversationPublicIds).hasSize(threadCount);
        assertThat(conversationPublicIds.stream().distinct().count()).isEqualTo(1);
    }
}
