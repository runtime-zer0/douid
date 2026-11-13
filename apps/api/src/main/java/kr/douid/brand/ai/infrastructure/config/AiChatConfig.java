package kr.douid.brand.ai.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import kr.douid.brand.ai.application.*;
import kr.douid.brand.ai.application.port.*;
import kr.douid.brand.ai.infrastructure.guard.InMemoryChatRequestGuard;
import kr.douid.brand.ai.infrastructure.stream.*;

/** AI Chat 정책·공급자·유한 실행 자원 연결 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiChatProperties.class)
public class AiChatConfig {
    /** 불변 정책 등록
     * @param properties 환경별 설정
     * @return application 정책
     */
    @Bean public AiChatPolicy aiChatPolicy(AiChatProperties properties) { return properties.toPolicy(); }
    /** 전역 guard 등록
     * @param policy 실행 정책
     * @return 요청 및 점유 제한
     */
    @Bean public ChatRequestGuard chatRequestGuard(AiChatPolicy policy) {
        return new InMemoryChatRequestGuard(policy, System::nanoTime);
    }
    /** 전용 유한 실행기 등록
     * @param policy 실행 정책
     * @return 종료 시 정리되는 실행기
     */
    @Bean(destroyMethod = "close")
    public BoundedChatStreamExecutor chatStreamExecutor(AiChatPolicy policy) {
        return new BoundedChatStreamExecutor(policy.maxConcurrentStreams());
    }
    /** 미준비 기본 공급자 등록
     * @return 외부 호출 없는 unavailable adapter
     */
    @Bean @ConditionalOnMissingBean(LlmGateway.class)
    public LlmGateway llmGateway() { return new UnavailableLlmGateway(); }
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
