package kr.douid.brand.client.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class ClientCredentialTest {

    @Test
    void isValid_미만료_미폐기_true() {
        ClientCredential credential = ClientCredential.issue(1L, "hash", LocalDateTime.now().plusDays(1));

        assertThat(credential.isValid(LocalDateTime.now())).isTrue();
    }

    @Test
    void isValid_만료됨_false() {
        ClientCredential credential = ClientCredential.issue(1L, "hash", LocalDateTime.now().minusDays(1));

        assertThat(credential.isValid(LocalDateTime.now())).isFalse();
    }

    @Test
    void isValid_폐기됨_만료전이어도_false() {
        ClientCredential credential = ClientCredential.issue(1L, "hash", LocalDateTime.now().plusDays(1));
        credential.revoke();

        assertThat(credential.isValid(LocalDateTime.now())).isFalse();
    }

    @Test
    void isValid_폐기되고_만료됨_false() {
        ClientCredential credential = ClientCredential.issue(1L, "hash", LocalDateTime.now().minusDays(1));
        credential.revoke();

        assertThat(credential.isValid(LocalDateTime.now())).isFalse();
    }
}
