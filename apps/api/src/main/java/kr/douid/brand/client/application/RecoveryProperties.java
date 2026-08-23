package kr.douid.brand.client.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Magic Link Recovery 정책 바인딩
 *
 * @param tokenExpirationMinutes Recovery Token 만료 시간(분)
 * @param magicLinkBaseUrl       Magic Link의 기준 URL(raw token은 쿼리 파라미터로 덧붙인다)
 */
@ConfigurationProperties(prefix = "app.client.recovery")
public record RecoveryProperties(int tokenExpirationMinutes, String magicLinkBaseUrl) {
}
