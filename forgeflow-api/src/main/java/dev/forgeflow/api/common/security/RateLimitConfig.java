package dev.forgeflow.api.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link RateLimitFilter} scoped to the login/register endpoints only, so the rest
 * of the API is unaffected. Can be disabled entirely via forgeflow.security.rate-limit-enabled
 * (used in tests, where Testcontainers Redis adds latency we don't need for every test).
 */
@Configuration
public class RateLimitConfig {

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(
            RateLimiter rateLimiter,
            @Value("${forgeflow.security.rate-limit-enabled:true}") boolean enabled) {

        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RateLimitFilter(rateLimiter));
        registration.addUrlPatterns("/api/v1/auth/login", "/api/v1/auth/register");
        registration.setEnabled(enabled);
        registration.setOrder(1);
        return registration;
    }
}
