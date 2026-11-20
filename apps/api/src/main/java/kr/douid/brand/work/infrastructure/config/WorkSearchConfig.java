package kr.douid.brand.work.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import kr.douid.brand.work.application.query.PortfolioSearchTermResolver;
import kr.douid.brand.work.application.query.PublicWorkSearchPolicy;

/**
 * 공개 작업물 검색 정책과 질의어 규칙 연결
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(WorkSearchProperties.class)
public class WorkSearchConfig {

    /**
     * 검색 결과 개수 정책 등록
     *
     * @param properties 검색 환경 설정
     * @return 검증된 검색 정책
     */
    @Bean
    public PublicWorkSearchPolicy publicWorkSearchPolicy(WorkSearchProperties properties) {
        return properties.toPolicy();
    }

    /**
     * 기본 동의어 표를 쓰는 질의어 규칙 등록
     *
     * @return 질의어 추출 정책 객체
     */
    @Bean
    public PortfolioSearchTermResolver portfolioSearchTermResolver() {
        return new PortfolioSearchTermResolver();
    }
}
