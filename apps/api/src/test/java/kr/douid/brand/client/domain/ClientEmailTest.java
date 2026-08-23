package kr.douid.brand.client.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class ClientEmailTest {

    @Test
    void verify_검증완료_상태로_생성된다() {
        LocalDateTime verifiedAt = LocalDateTime.now();

        ClientEmail clientEmail = ClientEmail.verify(1L, "User@Example.com", "user@example.com", verifiedAt);

        assertThat(clientEmail.getClientIdentityId()).isEqualTo(1L);
        assertThat(clientEmail.getEmail()).isEqualTo("User@Example.com");
        assertThat(clientEmail.getNormalizedEmail()).isEqualTo("user@example.com");
        assertThat(clientEmail.getVerifiedAt()).isEqualTo(verifiedAt);
    }

    @Test
    void isOwnedBy_동일_상담주체_true() {
        ClientEmail clientEmail = ClientEmail.verify(1L, "user@example.com", "user@example.com",
                LocalDateTime.now());

        assertThat(clientEmail.isOwnedBy(1L)).isTrue();
    }

    @Test
    void isOwnedBy_다른_상담주체_false() {
        ClientEmail clientEmail = ClientEmail.verify(1L, "user@example.com", "user@example.com",
                LocalDateTime.now());

        assertThat(clientEmail.isOwnedBy(2L)).isFalse();
    }
}
