package kr.douid.brand.shared.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Session Cookie 기반 인증 요청을 허용할 Origin 목록 바인딩
 *
 * @param allowedOrigins credential 포함 요청을 허용할 Origin 목록 (와일드카드 미허용)
 */
@ConfigurationProperties(prefix = "app.security")
public record CorsProperties(List<String> allowedOrigins) {
}
