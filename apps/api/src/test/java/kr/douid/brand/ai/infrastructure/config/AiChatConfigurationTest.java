package kr.douid.brand.ai.infrastructure.config;

import static org.assertj.core.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import kr.douid.brand.ai.application.*;
import kr.douid.brand.ai.application.port.LlmGateway;
import kr.douid.brand.ai.infrastructure.stream.UnavailableLlmGateway;
import kr.douid.brand.shared.config.CorsProperties;

class AiChatConfigurationTest {
    final ApplicationContextRunner runner=new ApplicationContextRunner()
            .withUserConfiguration(AiChatConfig.class,Origins.class);
    @Configuration(proxyBeanMethods=false) @EnableConfigurationProperties(CorsProperties.class)
    static class Origins {}
    @Test void 명시적_가짜설정만으로_adapter를_선택하며_시작시_네트워크접근_없음() {
        runner.withPropertyValues("app.ai-chat.openai.enabled=true", "app.ai-chat.openai.api-key=FAKE_KEY_CANARY")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(LlmGateway.class)).isInstanceOf(
                            kr.douid.brand.ai.infrastructure.openai.OpenAiResponsesGateway.class);
                    var client=context.getBean("openAiHttpClient", java.net.http.HttpClient.class);
                    assertThat(client.followRedirects()).isEqualTo(java.net.http.HttpClient.Redirect.NEVER);
                });
    }
    @Test void 활성화했어도_빈Key면_AI만_미준비() {
        runner.withPropertyValues("app.ai-chat.openai.enabled=true", "app.ai-chat.openai.api-key=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(LlmGateway.class)).isInstanceOf(UnavailableLlmGateway.class);
                    assertThat(context.getBean(AiChatPolicy.class)).isNotNull();
                });
    }
    @Test void 서로다른_두환경_정책과_Origin에도_운영_default는_가짜답변없이_503() {
        for(int i:new int[]{1,2}) runner.withPropertyValues("app.security.allowed-origins=https://site"+i+".example",
                "app.ai-chat.max-message-code-points="+(100*i),"app.ai-chat.rate-limit.requests="+i,
                "app.ai-chat.max-concurrent-streams="+i,"app.ai-chat.execution-timeout="+i+"s",
                "app.ai-chat.emitter-timeout="+(i+1)+"s").run(context->{
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(CorsProperties.class).allowedOrigins()).containsExactly("https://site"+i+".example");
            var policy=context.getBean(AiChatPolicy.class);
            assertThat(policy.maxMessageCodePoints()).isEqualTo(100*i);
            assertThat(policy.maxConcurrentStreams()).isEqualTo(i);
            assertThat(policy.executionTimeout().toSeconds()).isEqualTo(i);
            assertThat(context.getBean(LlmGateway.class)).isInstanceOf(UnavailableLlmGateway.class);
            assertThatThrownBy(()->context.getBean(AiChatUseCase.class).prepare(new AiChatCommand("hi",List.of())))
                    .isInstanceOf(AiChatException.Unavailable.class);
        });
    }
}
