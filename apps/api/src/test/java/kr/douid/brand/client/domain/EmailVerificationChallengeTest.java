package kr.douid.brand.client.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class EmailVerificationChallengeTest {

    private static final String CODE_HASH = "hash-of-123456";

    @Test
    void verify_올바른코드_성공하면_consumedAt이_기록된다() {
        EmailVerificationChallenge challenge = EmailVerificationChallenge.issue(1L, "user@example.com", CODE_HASH,
                LocalDateTime.now().plusMinutes(5));
        LocalDateTime now = LocalDateTime.now();

        challenge.verify(CODE_HASH, now);

        assertThat(challenge.getConsumedAt()).isEqualTo(now);
    }

    @Test
    void verify_만료된챌린지_VerificationCodeExpiredException() {
        EmailVerificationChallenge challenge = EmailVerificationChallenge.issue(1L, "user@example.com", CODE_HASH,
                LocalDateTime.now().minusMinutes(1));

        assertThatThrownBy(() -> challenge.verify(CODE_HASH, LocalDateTime.now()))
                .isInstanceOf(VerificationCodeExpiredException.class);
    }

    @Test
    void verify_이미소비된챌린지_VerificationCodeExpiredException() {
        EmailVerificationChallenge challenge = EmailVerificationChallenge.issue(1L, "user@example.com", CODE_HASH,
                LocalDateTime.now().plusMinutes(5));
        challenge.verify(CODE_HASH, LocalDateTime.now());

        assertThatThrownBy(() -> challenge.verify(CODE_HASH, LocalDateTime.now()))
                .isInstanceOf(VerificationCodeExpiredException.class);
    }

    @Test
    void verify_시도횟수초과_VerificationCodeExpiredException() {
        EmailVerificationChallenge challenge = EmailVerificationChallenge.issue(1L, "user@example.com", CODE_HASH,
                LocalDateTime.now().plusMinutes(5));

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> challenge.verify("wrong-hash", LocalDateTime.now()))
                    .isInstanceOf(VerificationCodeInvalidException.class);
        }

        assertThatThrownBy(() -> challenge.verify(CODE_HASH, LocalDateTime.now()))
                .isInstanceOf(VerificationCodeExpiredException.class);
    }

    @Test
    void verify_코드불일치_VerificationCodeInvalidException_시도횟수증가() {
        EmailVerificationChallenge challenge = EmailVerificationChallenge.issue(1L, "user@example.com", CODE_HASH,
                LocalDateTime.now().plusMinutes(5));

        assertThatThrownBy(() -> challenge.verify("wrong-hash", LocalDateTime.now()))
                .isInstanceOf(VerificationCodeInvalidException.class);

        assertThat(challenge.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void isConsumable_소비전이고_미만료_시도가능_true() {
        EmailVerificationChallenge challenge = EmailVerificationChallenge.issue(1L, "user@example.com", CODE_HASH,
                LocalDateTime.now().plusMinutes(5));

        assertThat(challenge.isConsumable(LocalDateTime.now())).isTrue();
    }

    @Test
    void isConsumable_이미소비됨_false() {
        EmailVerificationChallenge challenge = EmailVerificationChallenge.issue(1L, "user@example.com", CODE_HASH,
                LocalDateTime.now().plusMinutes(5));
        challenge.verify(CODE_HASH, LocalDateTime.now());

        assertThat(challenge.isConsumable(LocalDateTime.now())).isFalse();
    }
}
