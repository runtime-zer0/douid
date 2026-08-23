package kr.douid.brand.client.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.douid.brand.client.domain.ClientCredential;
import kr.douid.brand.client.domain.ClientCredentialRepository;

@ExtendWith(MockitoExtension.class)
class ClientAuthenticationServiceTest {

    @Mock
    private ClientCredentialRepository clientCredentialRepository;

    private final ClientCredentialIssuer clientCredentialIssuer = new ClientCredentialIssuer();

    private ClientAuthenticationService clientAuthenticationService;

    @BeforeEach
    void setUp() {
        clientAuthenticationService =
                new ClientAuthenticationService(clientCredentialRepository, clientCredentialIssuer);
    }

    @Test
    void resolve_DB에_존재하지않는_토큰_인증되지않음() {
        given(clientCredentialRepository.findByTokenHash(anyString()))
                .willReturn(Optional.empty());

        Optional<Long> result = clientAuthenticationService.resolve("unknown-raw-token");

        assertThat(result).isEmpty();
    }

    @Test
    void resolve_만료된_credential_인증되지않음() {
        ClientCredential expired = ClientCredential.issue(1L, "hash", LocalDateTime.now().minusDays(1));
        given(clientCredentialRepository.findByTokenHash(anyString()))
                .willReturn(Optional.of(expired));

        Optional<Long> result = clientAuthenticationService.resolve("raw-token");

        assertThat(result).isEmpty();
    }

    @Test
    void resolve_폐기된_credential_인증되지않음() {
        ClientCredential revoked = ClientCredential.issue(1L, "hash", LocalDateTime.now().plusDays(1));
        revoked.revoke();
        given(clientCredentialRepository.findByTokenHash(anyString()))
                .willReturn(Optional.of(revoked));

        Optional<Long> result = clientAuthenticationService.resolve("raw-token");

        assertThat(result).isEmpty();
    }

    @Test
    void resolve_유효한_credential_상담주체ID반환() {
        ClientCredential valid = ClientCredential.issue(42L, "hash", LocalDateTime.now().plusDays(1));
        given(clientCredentialRepository.findByTokenHash(anyString()))
                .willReturn(Optional.of(valid));

        Optional<Long> result = clientAuthenticationService.resolve("raw-token");

        assertThat(result).contains(42L);
    }
}
