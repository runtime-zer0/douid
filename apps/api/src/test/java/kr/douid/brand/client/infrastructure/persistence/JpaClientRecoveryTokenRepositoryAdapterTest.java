package kr.douid.brand.client.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import kr.douid.brand.client.domain.ClientRecoveryToken;
import kr.douid.brand.shared.config.JpaConfig;
import kr.douid.brand.shared.testsupport.PostgresIntegrationTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, JpaClientRecoveryTokenRepositoryAdapter.class})
class JpaClientRecoveryTokenRepositoryAdapterTest extends PostgresIntegrationTest {

    @Autowired
    private JpaClientRecoveryTokenRepositoryAdapter adapter;

    @Test
    void findByTokenHashForUpdate_저장한토큰을_해시로_조회할수있다() {
        adapter.save(ClientRecoveryToken.issue(1L, 10L, "token-hash", LocalDateTime.now().plusMinutes(15)));

        Optional<ClientRecoveryToken> found = adapter.findByTokenHashForUpdate("token-hash");

        assertThat(found).isPresent();
        assertThat(found.get().getClientIdentityId()).isEqualTo(1L);
    }

    @Test
    void findByTokenHashForUpdate_존재하지않으면_empty() {
        Optional<ClientRecoveryToken> found = adapter.findByTokenHashForUpdate("unknown-hash");

        assertThat(found).isEmpty();
    }
}
