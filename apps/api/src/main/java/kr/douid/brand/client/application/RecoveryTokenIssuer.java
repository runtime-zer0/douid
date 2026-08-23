package kr.douid.brand.client.application;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Magic Link Recovery Token 생성/해시 정책
 *
 * {@link ClientCredentialIssuer}와 동일한 {@code SecureRandom} 256비트 + URL-safe Base64 패턴을
 * 재사용한다. raw token은 어떤 로그에도 남기지 않는다(FR-016, FR-017, FR-033).
 */
@Component
@RequiredArgsConstructor
public class RecoveryTokenIssuer {

    private final ClientCredentialIssuer clientCredentialIssuer;

    /**
     * 예측 불가능한 raw Recovery Token을 생성
     *
     * @return URL-safe Base64로 인코딩된 raw token
     */
    public String issueRawToken() {
        return clientCredentialIssuer.issueRawToken();
    }

    /**
     * raw token을 저장용 해시로 변환
     *
     * @param rawToken 해시할 raw token
     * @return SHA-256 해시(hex 64자)
     */
    public String hash(String rawToken) {
        return clientCredentialIssuer.hash(rawToken);
    }
}
