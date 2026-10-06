package dev.forgeflow.api.common.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Simple fixed-window rate limiter backed by Redis INCR + EXPIRE. Deliberately fails OPEN:
 * if Redis is unreachable, {@link #isAllowed} returns true rather than blocking all traffic.
 * This is a documented trade-off (see README security section) favoring availability over
 * strict enforcement for an auth-endpoint-only limiter.
 */
@Component
public class RateLimiter {

    private final StringRedisTemplate redisTemplate;

    public RateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String key, int maxRequests, Duration window) {
        try {
            String redisKey = "ratelimit:" + key;
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count == null) {
                return true;
            }
            if (count == 1L) {
                redisTemplate.expire(redisKey, window);
            }
            return count <= maxRequests;
        } catch (Exception ex) {
            // Fail open: a Redis outage should not take down auth entirely.
            return true;
        }
    }
}
