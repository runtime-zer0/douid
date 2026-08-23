package kr.douid.brand.client.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

/**
 * client_token 생성/해시 정책
 *
 * raw token은 {@link SecureRandom} 256비트 난수를 URL-safe Base64로 인코딩해 생성하고,
 * 저장용 해시는 SHA-256(단방향)으로 변환한다.
 */
@Component
public class ClientCredentialIssuer {

    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final String HASH_ALGORITHM = "SHA-256";

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 예측 불가능한 raw token을 생성
     *
     * @return URL-safe Base64로 인코딩된 raw token
     */
    public String issueRawToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * raw token을 저장용 해시로 변환
     *
     * @param rawToken 해시할 raw token
     * @return SHA-256 해시(hex 64자)
     */
    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(HASH_ALGORITHM + " 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
