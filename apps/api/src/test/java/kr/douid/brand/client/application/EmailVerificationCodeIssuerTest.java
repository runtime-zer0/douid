package kr.douid.brand.client.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailVerificationCodeIssuerTest {

    private final EmailVerificationCodeIssuer issuer =
            new EmailVerificationCodeIssuer(new ClientCredentialIssuer());

    @Test
    void issueRawCode_6자리_숫자_문자열을_생성한다() {
        String code = issuer.issueRawCode();

        assertThat(code).hasSize(6);
        assertThat(code).containsOnlyDigits();
    }

    @Test
    void issueRawCode_호출할때마다_다른_코드를_생성한다() {
        String first = issuer.issueRawCode();
        String second = issuer.issueRawCode();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void hash_동일한_코드는_항상_동일한_해시를_생성한다() {
        String code = "123456";

        assertThat(issuer.hash(code)).isEqualTo(issuer.hash(code));
    }

    @Test
    void hash_다른_코드는_다른_해시를_생성한다() {
        assertThat(issuer.hash("123456")).isNotEqualTo(issuer.hash("654321"));
    }
}
