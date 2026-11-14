package kr.douid.brand.ai.infrastructure.config;

import java.net.URI;
import java.time.Duration;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 비밀값 문자열화를 제공하지 않는 서버 전용 Provider 설정 */
@Getter @Setter
@ConfigurationProperties("app.ai-chat.openai")
public final class OpenAiProperties {
    private boolean enabled;
    private String apiKey;
    private String model = "gpt-6-luna";
    private int maxInputTokens = 8192;
    private int maxOutputTokens = 1024;
    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration responseHeaderTimeout = Duration.ofSeconds(10);
    private Duration countTimeout = Duration.ofSeconds(10);
    private Duration readIdleTimeout = Duration.ofSeconds(15);
    private int maxProviderBodyBytes = 2_097_152;
    private int maxProviderEventBytes = 131_072;
    private int maxCountBodyBytes = 4096;
    private int maxProviderRequestBytes = 32768;
    private String kakaoChannelUrl;

    @PostConstruct
    private void validateDefaults() { validate(Duration.ofSeconds(60)); }

    /** 네트워크 접근 없는 활성화 가능 여부
     * @return 명시 활성화와 비어 있지 않은 서버 credential 유무
     */
    public boolean ready() { return enabled && apiKey != null && !apiKey.isBlank(); }

    /** 유한 한도와 승인 모델 검증
     * @param totalTimeout 기존 전체 실행 기한
     * @throws IllegalArgumentException 무효한 서버 정책
     */
    public void validate(Duration totalTimeout) {
        if (!"gpt-6-luna".equals(model) || maxInputTokens < 1 || maxInputTokens > 8192
                || maxOutputTokens < 1 || maxOutputTokens > 1024
                || (long) maxInputTokens + maxOutputTokens > 1_050_000
                || maxProviderBodyBytes < 1 || maxProviderBodyBytes > 2_097_152
                || maxProviderEventBytes < 1 || maxProviderEventBytes > maxProviderBodyBytes
                || maxCountBodyBytes < 1 || maxCountBodyBytes > 4096
                || maxProviderRequestBytes < 1 || maxProviderRequestBytes > 32768) invalid();
        for (var timeout : new Duration[]{connectTimeout, responseHeaderTimeout, countTimeout, readIdleTimeout}) {
            if (timeout == null || timeout.isNegative() || timeout.isZero() || timeout.compareTo(totalTimeout) > 0) invalid();
        }
        if (kakaoChannelUrl != null && !kakaoChannelUrl.isBlank()) {
            try {
                var uri = URI.create(kakaoChannelUrl);
                if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                        || uri.getFragment() != null || uri.getPort() != -1) invalid();
            } catch (IllegalArgumentException e) { invalid(); }
        }
    }

    private static void invalid() { throw new IllegalArgumentException("Invalid OpenAI resource policy"); }
}
