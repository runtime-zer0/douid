package kr.douid.brand.client.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RecoveryTokenIssuerTest {

    private final RecoveryTokenIssuer issuer = new RecoveryTokenIssuer(new ClientCredentialIssuer());

    @Test
    void issueRawToken_예측_불가능한_토큰을_생성한다() {
        String first = issuer.issueRawToken();
        String second = issuer.issueRawToken();

        assertThat(first).isNotEqualTo(second);
        assertThat(first).isNotBlank();
    }

    @Test
    void hash_동일한_토큰은_항상_동일한_해시를_생성한다() {
        String rawToken = issuer.issueRawToken();

        assertThat(issuer.hash(rawToken)).isEqualTo(issuer.hash(rawToken));
    }

    @Test
    void hash_다른_토큰은_다른_해시를_생성한다() {
        String first = issuer.issueRawToken();
        String second = issuer.issueRawToken();

        assertThat(issuer.hash(first)).isNotEqualTo(issuer.hash(second));
    }
}
