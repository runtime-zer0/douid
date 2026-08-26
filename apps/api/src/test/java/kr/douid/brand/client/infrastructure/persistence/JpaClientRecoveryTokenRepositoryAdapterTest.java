package kr.douid.brand.client.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import kr.douid.brand.client.domain.ClientEmail;
import kr.douid.brand.client.domain.ClientIdentity;
import kr.douid.brand.client.domain.ClientRecoveryToken;
import kr.douid.brand.shared.config.JpaConfig;
import kr.douid.brand.shared.testsupport.PostgresIntegrationTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, JpaClientRecoveryTokenRepositoryAdapter.class})
class JpaClientRecoveryTokenRepositoryAdapterTest extends PostgresIntegrationTest {

    @Autowired
    private JpaClientRecoveryTokenRepositoryAdapter adapter;

    @Autowired
    private ClientIdentityJpaRepository clientIdentityJpaRepository;

    @Autowired
    private ClientEmailJpaRepository clientEmailJpaRepository;

    private Long clientIdentityId;
    private Long clientEmailId;

    @BeforeEach
    void setUp() {
        clientIdentityId = clientIdentityJpaRepository.save(ClientIdentity.create()).getId();
        clientEmailId = clientEmailJpaRepository.save(
                ClientEmail.verify(clientIdentityId, "user@example.com", "user@example.com", LocalDateTime.now()))
                .getId();
    }

    @Test
    void findByTokenHashForUpdate_저장한토큰을_해시로_조회할수있다() {
        adapter.save(ClientRecoveryToken.issue(clientIdentityId, clientEmailId, "token-hash",
                LocalDateTime.now().plusMinutes(15)));

        Optional<ClientRecoveryToken> found = adapter.findByTokenHashForUpdate("token-hash");

        assertThat(found).isPresent();
        assertThat(found.get().getClientIdentityId()).isEqualTo(clientIdentityId);
    }

    @Test
    void findByTokenHashForUpdate_존재하지않으면_empty() {
        Optional<ClientRecoveryToken> found = adapter.findByTokenHashForUpdate("unknown-hash");

        assertThat(found).isEmpty();
    }
}
