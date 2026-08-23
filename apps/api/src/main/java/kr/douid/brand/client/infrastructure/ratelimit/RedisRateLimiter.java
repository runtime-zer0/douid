package kr.douid.brand.client.infrastructure.ratelimit;

import java.time.Duration;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import kr.douid.brand.client.application.port.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis {@code INCR}+{@code EXPIRE} 기반 고정 윈도우 카운터 rate limiter
 *
 * 키의 최초 증가 시에만 만료 시간을 설정해 윈도우를 고정한다. Redis 연결 실패 시에는 rate limit을
 * 건너뛰고 요청을 통과시킨다(fail-open) — 코드/토큰 자체의 시도 횟수·유효기간 제한이 1차 방어선이므로,
 * rate limit이 일시적으로 느슨해져도 치명적인 보안 공백은 아니라고 판단했다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisRateLimiter implements RateLimiter {

    private final RedisTemplate<String, Long> rateLimitRedisTemplate;

    @Override
    public boolean tryConsume(String key, int limit, Duration window) {
        try {
            Long count = rateLimitRedisTemplate.opsForValue().increment(key);
            if (count == null) {
                return true;
            }
            if (count == 1L) {
                rateLimitRedisTemplate.expire(key, window);
            }
            return count <= limit;
        } catch (DataAccessException e) {
            log.warn("Redis rate limit 체크 실패, fail-open으로 요청을 통과시킵니다.", e);
            return true;
        }
    }
}
