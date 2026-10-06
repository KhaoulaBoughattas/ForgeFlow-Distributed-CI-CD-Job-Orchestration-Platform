package dev.forgeflow.api.common.security;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises RateLimiter against a standalone Redis container rather than the full Spring
 * context, since the rule under test (fixed-window INCR+EXPIRE) does not need anything else
 * the application context would provide.
 */
class RateLimiterTest {

    private static GenericContainer<?> redis;
    private static RateLimiter rateLimiter;

    @BeforeAll
    static void startRedis() {
        redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
        redis.start();

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redis.getHost(), redis.getMappedPort(6379));
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();

        StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
        rateLimiter = new RateLimiter(redisTemplate);
    }

    @AfterAll
    static void stopRedis() {
        redis.stop();
    }

    @Test
    void allowsRequestsUpToTheLimit() {
        String key = "test-key-" + System.nanoTime();
        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.isAllowed(key, 5, Duration.ofSeconds(30))).isTrue();
        }
    }

    @Test
    void blocksRequestsOverTheLimit() {
        String key = "test-key-" + System.nanoTime();
        for (int i = 0; i < 3; i++) {
            rateLimiter.isAllowed(key, 3, Duration.ofSeconds(30));
        }
        assertThat(rateLimiter.isAllowed(key, 3, Duration.ofSeconds(30))).isFalse();
    }
}
