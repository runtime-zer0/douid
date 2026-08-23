package kr.douid.brand.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * client_token 쿠키 정책 바인딩
 *
 * @param tokenExpirationDays credential 만료 기간(일)
 * @param cookieSecure        쿠키 Secure 속성 (dev는 false, prod는 true)
 */
@ConfigurationProperties(prefix = "app.client")
public record ClientCookieProperties(int tokenExpirationDays, boolean cookieSecure) {
}
