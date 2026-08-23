package kr.douid.brand.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Rate limit 카운터 저장에 사용할 {@link RedisTemplate} 빈 설정
 *
 * 도메인 중립적인 기술 설정이라 {@code shared.config}에 두고, 이를 사용하는 rate limit 알고리즘
 * 자체는 {@code client.infrastructure.ratelimit}에 둔다.
 */
@Configuration
public class RedisConfig {

    /**
     * rate limit 카운터 전용 RedisTemplate 빈 등록
     *
     * @param connectionFactory Redis 연결 팩토리
     * @return 문자열 키, Long 값을 직렬화하는 {@link RedisTemplate}
     */
    @Bean
    public RedisTemplate<String, Long> rateLimitRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Long> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericToStringSerializer<>(Long.class));
        template.afterPropertiesSet();
        return template;
    }
}
