package com.waiveliability.common.ratelimit;

import com.waiveliability.common.exception.RateLimitException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    /**
     * Rate limit check using sliding window counter in Redis.
     * @param key The rate limit key (e.g., IP address)
     * @throws RateLimitException if the request limit is exceeded
     */
    public void checkRateLimit(String key) {
        String redisKey = "rate_limit:" + key;

        Long count = redisTemplate.opsForValue().increment(redisKey);

        if (count != null && count == 1) {
            // First request in this window, set expiration
            redisTemplate.expire(redisKey, WINDOW);
        }

        if (count != null && count > MAX_REQUESTS_PER_MINUTE) {
            throw new RateLimitException("Too many requests. Please try again later.");
        }
    }
}
