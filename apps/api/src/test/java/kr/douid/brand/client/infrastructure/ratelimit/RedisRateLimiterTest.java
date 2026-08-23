package kr.douid.brand.client.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class RedisRateLimiterTest {

    private static final GenericContainer<?> REDIS_CONTAINER =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private static RedisTemplate<String, Long> redisTemplate;

    @BeforeAll
    static void setUpRedis() {
        REDIS_CONTAINER.start();

        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(
                REDIS_CONTAINER.getHost(), REDIS_CONTAINER.getMappedPort(6379));
        connectionFactory = new LettuceConnectionFactory(configuration);
        connectionFactory.afterPropertiesSet();

        redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericToStringSerializer<>(Long.class));
        redisTemplate.afterPropertiesSet();
    }

    @AfterAll
    static void tearDownRedis() {
        connectionFactory.destroy();
        REDIS_CONTAINER.stop();
    }

    @Test
    void tryConsume_제한이내_true를_반환한다() {
        RedisRateLimiter rateLimiter = new RedisRateLimiter(redisTemplate);
        String key = "test:limit-within:" + System.nanoTime();

        boolean result = rateLimiter.tryConsume(key, 5, Duration.ofMinutes(1));

        assertThat(result).isTrue();
    }

    @Test
    void tryConsume_제한초과_false를_반환한다() {
        RedisRateLimiter rateLimiter = new RedisRateLimiter(redisTemplate);
        String key = "test:limit-exceeded:" + System.nanoTime();

        for (int i = 0; i < 3; i++) {
            assertThat(rateLimiter.tryConsume(key, 3, Duration.ofMinutes(1))).isTrue();
        }

        assertThat(rateLimiter.tryConsume(key, 3, Duration.ofMinutes(1))).isFalse();
    }

    @Test
    void tryConsume_윈도우만료후_다시허용된다() throws InterruptedException {
        RedisRateLimiter rateLimiter = new RedisRateLimiter(redisTemplate);
        String key = "test:window-expiry:" + System.nanoTime();

        assertThat(rateLimiter.tryConsume(key, 1, Duration.ofSeconds(1))).isTrue();
        assertThat(rateLimiter.tryConsume(key, 1, Duration.ofSeconds(1))).isFalse();

        Thread.sleep(1500);

        assertThat(rateLimiter.tryConsume(key, 1, Duration.ofSeconds(1))).isTrue();
    }

    @Test
    void tryConsume_Redis연결불가_failOpen으로_true를_반환한다() {
        LettuceConnectionFactory brokenConnectionFactory =
                new LettuceConnectionFactory(new RedisStandaloneConfiguration("localhost", 1));
        brokenConnectionFactory.afterPropertiesSet();

        RedisTemplate<String, Long> brokenTemplate = new RedisTemplate<>();
        brokenTemplate.setConnectionFactory(brokenConnectionFactory);
        brokenTemplate.setKeySerializer(new StringRedisSerializer());
        brokenTemplate.setValueSerializer(new GenericToStringSerializer<>(Long.class));
        brokenTemplate.afterPropertiesSet();

        RedisRateLimiter rateLimiter = new RedisRateLimiter(brokenTemplate);

        boolean result = rateLimiter.tryConsume("test:fail-open", 1, Duration.ofMinutes(1));

        assertThat(result).isTrue();

        brokenConnectionFactory.destroy();
    }
}
