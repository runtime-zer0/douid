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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import kr.douid.brand.client.application.ClientIdentityProvisioningService;

/**
 * 동일 상담 주체에서 거의 동시에 여러 상담 시작 요청이 발생해도 활성 Conversation이 하나만
 * 존재하는지 검증(FR-026, research.md #7 비관적 락 검증)
 */
@SpringBootTest
@Testcontainers
class ConversationStartServiceConcurrencyTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("spring.sql.init.mode", () -> "never");
    }

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
