package kr.douid.brand.ai.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class AiChatPropertiesTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Config.class);

    @Test
    void 기본값과_환경별_바인딩() {
        runner.run(context -> {
            var p = context.getBean(AiChatProperties.class);
            assertThat(p.maxRequestBytes()).isEqualTo(16384);
            assertThat(p.rateLimit().requests()).isEqualTo(10);
            assertThat(p.maxConcurrentStreams()).isEqualTo(2);
        });
        runner.withPropertyValues("app.ai-chat.max-request-bytes=512", "app.ai-chat.rate-limit.requests=2")
                .run(context -> {
                    assertThat(context.getBean(AiChatProperties.class).maxRequestBytes()).isEqualTo(512);
                    assertThat(context.getBean(AiChatProperties.class).rateLimit().requests()).isEqualTo(2);
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"max-request-bytes=0", "max-request-bytes=2147483647",
            "max-message-code-points=0", "max-history-messages=-1", "max-history-message-code-points=0",
            "max-context-code-points=1", "rate-limit.requests=0", "rate-limit.window=0s",
            "max-concurrent-streams=0", "max-concurrent-streams=65", "execution-timeout=0s",
            "emitter-timeout=59s", "max-output-bytes=0", "max-delta-events=0"})
    void 잘못된_제한은_시작시_거부(String property) {
        runner.withPropertyValues("app.ai-chat." + property).run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AiChatProperties.class)
    static class Config {}
}
