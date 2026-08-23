package kr.douid.brand.client.application;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 이메일 인증 코드 생성/해시 정책
 *
 * raw 코드는 6자리 숫자를 {@link SecureRandom}으로 생성하고, 저장용 해시는
 * {@link ClientCredentialIssuer#hash(String)}을 재사용한다(SHA-256). raw 코드는 어떤 로그에도 남기지 않는다.
 */
@Component
@RequiredArgsConstructor
public class EmailVerificationCodeIssuer {

    private static final int CODE_LENGTH = 6;
    private static final int CODE_BOUND = 1_000_000;

    private final ClientCredentialIssuer clientCredentialIssuer;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 예측 불가능한 6자리 숫자 코드를 생성
     *
     * @return 0으로 패딩된 6자리 숫자 문자열
     */
    public String issueRawCode() {
        int value = secureRandom.nextInt(CODE_BOUND);
        return String.format("%0" + CODE_LENGTH + "d", value);
    }

    /**
     * raw 코드를 저장용 해시로 변환
     *
     * @param rawCode 해시할 raw 코드
     * @return SHA-256 해시(hex 64자)
     */
    public String hash(String rawCode) {
        return clientCredentialIssuer.hash(rawCode);
    }
}
