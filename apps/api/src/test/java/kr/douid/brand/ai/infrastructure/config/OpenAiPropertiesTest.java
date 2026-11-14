package kr.douid.brand.ai.infrastructure.config;

import static org.assertj.core.api.Assertions.*;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class OpenAiPropertiesTest {
    final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Config.class);
    @Configuration(proxyBeanMethods = false) @EnableConfigurationProperties(OpenAiProperties.class)
    static class Config {}

    @Test void 두_환경의_비밀아닌_Token과_timeout_바인딩() {
        for(int env:new int[]{1,2})runner.withPropertyValues("app.ai-chat.openai.max-input-tokens="+(env*1000),
                "app.ai-chat.openai.max-output-tokens="+(env*64),"app.ai-chat.openai.count-timeout="+env+"s",
                "app.ai-chat.openai.read-idle-timeout="+(env+1)+"s").run(c->{
            assertThat(c).hasNotFailed();var p=c.getBean(OpenAiProperties.class);
            assertThat(p.getMaxInputTokens()).isEqualTo(env*1000);assertThat(p.getMaxOutputTokens()).isEqualTo(env*64);
            assertThat(p.getCountTimeout()).isEqualTo(Duration.ofSeconds(env));
            assertThat(p.getReadIdleTimeout()).isEqualTo(Duration.ofSeconds(env+1));
            assertThat(p.getModel()).isEqualTo("gpt-6-luna");
        });
    }

    @Test void 기본_비활성과_유한_예산() {
        runner.run(c -> {
            assertThat(c).hasNotFailed();
            var p = c.getBean(OpenAiProperties.class);
            assertThat(p.ready()).isFalse();
            assertThat(p.getModel()).isEqualTo("gpt-6-luna");
            assertThat(p.getMaxInputTokens()).isEqualTo(8192);
            assertThat(p.getMaxOutputTokens()).isEqualTo(1024);
            p.validate(Duration.ofSeconds(60));
        });
    }
    @Test void Key가_없어도_기동하며_가짜_Key는_문자열화에_미노출() {
        runner.withPropertyValues("app.ai-chat.openai.enabled=true").run(c ->
                assertThat(c.getBean(OpenAiProperties.class).ready()).isFalse());
        runner.withPropertyValues("app.ai-chat.openai.enabled=true", "app.ai-chat.openai.api-key=KEY_CANARY")
                .run(c -> {
                    var p = c.getBean(OpenAiProperties.class);
                    assertThat(p.ready()).isTrue();
                    assertThat(p.toString()).doesNotContain("KEY_CANARY");
                });
    }
    @ParameterizedTest @ValueSource(strings = {"model=arbitrary", "max-input-tokens=8193",
            "max-output-tokens=1025", "max-input-tokens=0", "read-idle-timeout=0s",
            "max-provider-event-bytes=2097153", "kakao-channel-url=http://bad.example"})
    void 잘못된_설정은_안전한_고정_오류(String value) {
        runner.withPropertyValues("app.ai-chat.openai." + value).run(c -> {
            assertThat(c).hasFailed();
            assertThat(c.getStartupFailure()).hasRootCauseMessage("Invalid OpenAI resource policy");
        });
    }
}
