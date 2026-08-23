package kr.douid.brand.shared.security;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import kr.douid.brand.shared.config.ClientCookieProperties;
import lombok.RequiredArgsConstructor;

/**
 * client_token Persistent Cookie 발급을 담당하는 provider
 */
@Component
@RequiredArgsConstructor
public class ClientTokenCookieProvider {

    private static final String COOKIE_NAME = "client_token";

    private final ClientCookieProperties clientCookieProperties;

    /**
     * raw token으로 브라우저에 내려줄 쿠키를 생성
     *
     * @param rawToken 발급된 raw token
     * @return {@code HttpOnly}/{@code SameSite=Lax} 속성을 가진 Persistent Cookie
     */
    public ResponseCookie issueCookie(String rawToken) {
        return ResponseCookie.from(COOKIE_NAME, rawToken)
                .httpOnly(true)
                .secure(clientCookieProperties.cookieSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(clientCookieProperties.tokenExpirationDays()))
                .build();
    }
}
