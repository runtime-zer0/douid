package kr.douid.brand.work.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import kr.douid.brand.work.application.query.PublicWorkSearchPolicy;

/**
 * 공개 작업물 검색 환경 설정
 *
 * @param maxResults 최대 결과 수
 */
@ConfigurationProperties("app.work-search")
public record WorkSearchProperties(@DefaultValue("5") int maxResults) {

    /**
     * 검색 정책 변환
     *
     * @return 검증된 검색 정책
     * @throws IllegalArgumentException 결과 상한이 1~9 범위 밖일 때
     */
    public PublicWorkSearchPolicy toPolicy() {
        return new PublicWorkSearchPolicy(maxResults);
    }
}
