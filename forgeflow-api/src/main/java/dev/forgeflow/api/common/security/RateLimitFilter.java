package dev.forgeflow.api.common.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Duration;

/**
 * Plain servlet Filter (not a Spring Security filter) applying a per-client-IP rate limit.
 * Registered only on auth endpoints via {@code RateLimitConfig}.
 */
public class RateLimitFilter implements Filter {

    private static final int MAX_REQUESTS_PER_WINDOW = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final RateLimiter rateLimiter;

    public RateLimitFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientKey = clientIp(httpRequest) + ":" + httpRequest.getRequestURI();
        if (!rateLimiter.isAllowed(clientKey, MAX_REQUESTS_PER_WINDOW, WINDOW)) {
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"detail\":\"Too many requests, please try again later.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
