package kr.douid.brand.client.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailNormalizerTest {

    private final EmailNormalizer emailNormalizer = new EmailNormalizer();

    @Test
    void normalize_공백_제거되고_소문자로_변환된다() {
        String result = emailNormalizer.normalize("  User@Example.COM  ");

        assertThat(result).isEqualTo("user@example.com");
    }

    @Test
    void normalize_이미_정규화된_이메일_동일하게_반환된다() {
        String result = emailNormalizer.normalize("user@example.com");

        assertThat(result).isEqualTo("user@example.com");
    }
}
