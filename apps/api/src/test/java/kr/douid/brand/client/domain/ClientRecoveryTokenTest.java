package kr.douid.brand.client.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class ClientRecoveryTokenTest {

    @Test
    void consume_성공하면_consumedAt이_기록된다() {
        ClientRecoveryToken token = ClientRecoveryToken.issue(1L, 10L, "token-hash",
                LocalDateTime.now().plusMinutes(15));
        LocalDateTime now = LocalDateTime.now();

        token.consume(now);

        assertThat(token.getConsumedAt()).isEqualTo(now);
    }

    @Test
    void consume_만료된토큰_RecoveryTokenExpiredException() {
        ClientRecoveryToken token = ClientRecoveryToken.issue(1L, 10L, "token-hash",
                LocalDateTime.now().minusMinutes(1));

        assertThatThrownBy(() -> token.consume(LocalDateTime.now()))
                .isInstanceOf(RecoveryTokenExpiredException.class);
    }

    @Test
    void consume_이미소비된토큰_RecoveryTokenInvalidException() {
        ClientRecoveryToken token = ClientRecoveryToken.issue(1L, 10L, "token-hash",
                LocalDateTime.now().plusMinutes(15));
        token.consume(LocalDateTime.now());

        assertThatThrownBy(() -> token.consume(LocalDateTime.now()))
                .isInstanceOf(RecoveryTokenInvalidException.class);
    }
}
