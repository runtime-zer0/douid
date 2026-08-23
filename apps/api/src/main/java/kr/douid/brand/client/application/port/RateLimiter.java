package kr.douid.brand.client.application.port;

import java.time.Duration;

/**
 * 요청 빈도 제한을 위한 application port
 *
 * 구현체는 {@code client.infrastructure.ratelimit}에 두고, 실제 저장소(Redis 등) 세부사항을 감춘다.
 */
public interface RateLimiter {

    /**
     * 주어진 키에 대한 요청을 시도하고, 제한 범위 내인지 확인
     *
     * @param key    rate limit을 구분하는 키
     * @param limit  윈도우 내 허용 최대 요청 수
     * @param window 카운트 윈도우 길이
     * @return 제한 범위 내이면 true, 초과했으면 false
     */
    boolean tryConsume(String key, int limit, Duration window);
}
