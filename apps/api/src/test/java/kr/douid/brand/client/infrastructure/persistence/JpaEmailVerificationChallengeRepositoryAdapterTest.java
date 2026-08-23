package kr.douid.brand.client.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import kr.douid.brand.client.domain.EmailVerificationChallenge;
import kr.douid.brand.shared.config.JpaConfig;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, JpaEmailVerificationChallengeRepositoryAdapter.class})
class JpaEmailVerificationChallengeRepositoryAdapterTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private JpaEmailVerificationChallengeRepositoryAdapter adapter;

    @Test
    void findLatestByClientIdentityIdAndNormalizedEmail_가장최근챌린지를_반환한다() {
        adapter.save(EmailVerificationChallenge.issue(1L, "user@example.com", "old-hash",
                LocalDateTime.now().plusMinutes(5)));
        EmailVerificationChallenge latest = adapter.save(EmailVerificationChallenge.issue(1L, "user@example.com",
                "new-hash", LocalDateTime.now().plusMinutes(5)));

        Optional<EmailVerificationChallenge> found =
                adapter.findLatestByClientIdentityIdAndNormalizedEmail(1L, "user@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(latest.getId());
    }

    @Test
    void findLatestByClientIdentityIdAndNormalizedEmail_다른상담주체는_조회되지않는다() {
        adapter.save(EmailVerificationChallenge.issue(1L, "user@example.com", "hash",
                LocalDateTime.now().plusMinutes(5)));

        Optional<EmailVerificationChallenge> found =
                adapter.findLatestByClientIdentityIdAndNormalizedEmail(2L, "user@example.com");

        assertThat(found).isEmpty();
    }
}
