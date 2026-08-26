package kr.douid.brand.client.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import kr.douid.brand.client.domain.ClientEmail;
import kr.douid.brand.client.domain.ClientIdentity;
import kr.douid.brand.shared.config.JpaConfig;
import kr.douid.brand.shared.testsupport.PostgresIntegrationTest;

/**
 * 동시 트랜잭션 간 경합을 검증하는 테스트가 포함되어 있어 클래스 전체를
 * {@code Propagation.NOT_SUPPORTED}로 지정한다. {@code @DataJpaTest} 기본 트랜잭션 롤백에
 * 의존하면, {@code @BeforeEach}에서 저장한 데이터가 아직 커밋되지 않은 채로 남아 별도 스레드가
 * 여는 독립 트랜잭션(다른 커넥션)에서는 보이지 않는다(PostgreSQL READ COMMITTED 기본 동작).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, JpaClientEmailRepositoryAdapter.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class JpaClientEmailRepositoryAdapterTest extends PostgresIntegrationTest {

    @Autowired
    private JpaClientEmailRepositoryAdapter adapter;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ClientIdentityJpaRepository clientIdentityJpaRepository;

    private Long clientIdentityId;
    private Long otherClientIdentityId;

    @BeforeEach
    void setUp() {
        clientIdentityId = clientIdentityJpaRepository.save(ClientIdentity.create()).getId();
        otherClientIdentityId = clientIdentityJpaRepository.save(ClientIdentity.create()).getId();
    }

    @Test
    void lockByNormalizedEmail_두상담주체가동시에검증완료를시도해도_하나만성공한다() throws InterruptedException {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        String normalizedEmail = "race@example.com";
        CountDownLatch bothStarted = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicInteger successCount = new AtomicInteger();

        List<? extends Future<?>> futures = List.of(clientIdentityId, otherClientIdentityId).stream()
                .map(clientIdentityId -> executor.submit(() -> {
                    bothStarted.countDown();
                    transactionTemplate.executeWithoutResult(status -> {
                        adapter.lockByNormalizedEmail(normalizedEmail);
                        Optional<ClientEmail> alreadyOwned = adapter.findByNormalizedEmail(normalizedEmail);
                        if (alreadyOwned.isEmpty()) {
                            adapter.save(ClientEmail.verify(clientIdentityId, "race@example.com", normalizedEmail,
                                    LocalDateTime.now()));
                            successCount.incrementAndGet();
                        }
                    });
                }))
                .toList();

        for (Future<?> future : futures) {
            try {
                future.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(successCount.get()).isEqualTo(1);
        Optional<ClientEmail> found = adapter.findByNormalizedEmail(normalizedEmail);
        assertThat(found).isPresent();
    }

    @Test
    void save_저장한이메일을_normalizedEmail로_조회할수있다() {
        adapter.save(ClientEmail.verify(clientIdentityId, "User@Example.com", "user@example.com", LocalDateTime.now()));

        Optional<ClientEmail> found = adapter.findByNormalizedEmail("user@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getClientIdentityId()).isEqualTo(clientIdentityId);
    }

    @Test
    void findByNormalizedEmail_존재하지않으면_empty() {
        Optional<ClientEmail> found = adapter.findByNormalizedEmail("unknown@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    void findVerifiedByClientIdentityIdAndNormalizedEmail_본인소유만_조회된다() {
        adapter.save(ClientEmail.verify(clientIdentityId, "user@example.com", "user@example.com", LocalDateTime.now()));

        Optional<ClientEmail> ownedByOther =
                adapter.findVerifiedByClientIdentityIdAndNormalizedEmail(otherClientIdentityId, "user@example.com");
        Optional<ClientEmail> ownedBySelf =
                adapter.findVerifiedByClientIdentityIdAndNormalizedEmail(clientIdentityId, "user@example.com");

        assertThat(ownedByOther).isEmpty();
        assertThat(ownedBySelf).isPresent();
    }
}
