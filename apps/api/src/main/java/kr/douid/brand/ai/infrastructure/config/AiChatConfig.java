package kr.douid.brand.ai.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import kr.douid.brand.ai.application.*;
import kr.douid.brand.ai.application.port.*;
import kr.douid.brand.ai.infrastructure.guard.InMemoryChatRequestGuard;
import kr.douid.brand.ai.infrastructure.stream.*;
import java.net.http.HttpClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import kr.douid.brand.ai.infrastructure.openai.OpenAiResponsesGateway;

/** AI Chat 정책·공급자·유한 실행 자원 연결 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({AiChatProperties.class, OpenAiProperties.class})
public class AiChatConfig {
    /** 불변 정책 등록
     * @param properties 환경별 설정
     * @return application 정책
     */
    @Bean public AiChatPolicy aiChatPolicy(AiChatProperties properties) { return properties.toPolicy(); }
    /** 전역 guard 등록
     * @param policy 실행 정책
     * @param registries 관측 registry
     * @return 요청 및 점유 제한
     */
    @Bean public ChatRequestGuard chatRequestGuard(AiChatPolicy policy, ObjectProvider<MeterRegistry> registries) {
        return new InMemoryChatRequestGuard(policy, System::nanoTime, registries.getIfAvailable(SimpleMeterRegistry::new));
    }
    /** 전용 유한 실행기 등록
     * @param policy 실행 정책
     * @return 종료 시 정리되는 실행기
     */
    @Bean(destroyMethod = "close")
    public BoundedChatStreamExecutor chatStreamExecutor(AiChatPolicy policy) {
        return new BoundedChatStreamExecutor(policy.maxConcurrentStreams());
    }
    /** 요청과 분리된 공유 HTTP 연결 소유권
     * @param properties 연결 제한 설정
     * @return Spring 종료 시 닫히는 redirect 금지 client
     */
    @Bean(destroyMethod = "close")
    public HttpClient openAiHttpClient(OpenAiProperties properties) {
        return HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }
    /** 로컬 readiness에 따른 공급자 선택
     * @param properties 서버 전용 설정
     * @param policy 기존 전체 실행 기한
     * @param client 공유 client
     * @param executor 기존 예약 경계
     * @param registries 관측 registry
     * @return 외부 호출 없이 선택한 공급자
     */
    @Bean @ConditionalOnMissingBean(LlmGateway.class)
    public LlmGateway llmGateway(OpenAiProperties properties, AiChatPolicy policy,
            @Qualifier("openAiHttpClient") HttpClient client, ChatStreamExecutor executor,
            ObjectProvider<MeterRegistry> registries) {
        if (!properties.ready()) return new UnavailableLlmGateway();
        properties.validate(policy.executionTimeout());
        return new OpenAiResponsesGateway(properties, client, executor, registries.getIfAvailable(SimpleMeterRegistry::new));
    }
    /** application 수락 경계 등록
     * @param gateway 공급자 준비 경계
     * @param guard 요청 제한
     * @param policy 불변 정책
     * @param executor 유한 실행 경계
     * @return 검증·수락 use case
     */
    @Bean public AiChatUseCase aiChatUseCase(LlmGateway gateway, ChatRequestGuard guard, AiChatPolicy policy, ChatStreamExecutor executor) {
        return new AiChatUseCase(gateway, guard, policy, executor);
    }
}
