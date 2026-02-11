package com.pastudyhub.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter: 200 req/min per IP address.
 *
 * Production deployments should replace this with Redis-backed
 * Spring Cloud Gateway RequestRateLimiter.
 */
@Component
@Slf4j
public class InMemoryRateLimitFilter implements GlobalFilter, Ordered {

    private static final int MAX_REQUESTS_PER_MINUTE = 200;

    private final Map<String, WindowCounter> windows = new ConcurrentHashMap<>();

    @Override
    public int getOrder() {
        return -150;   // run before JWT filter
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String ip = resolveClientIp(exchange);
        WindowCounter counter = windows.computeIfAbsent(ip, k -> new WindowCounter());

        if (!counter.tryIncrement(Instant.now())) {
            log.warn("Rate limit exceeded for IP: {}", ip);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        var addr = exchange.getRequest().getRemoteAddress();
        return addr != null ? addr.getAddress().getHostAddress() : "unknown";
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    // -------------------------------------------------------------------------

    private static class WindowCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile Instant windowStart = Instant.now();

        synchronized boolean tryIncrement(Instant now) {
            if (now.getEpochSecond() - windowStart.getEpochSecond() >= 60) {
                windowStart = now;
                count.set(0);
            }
            if (count.get() >= MAX_REQUESTS_PER_MINUTE) return false;
            count.incrementAndGet();
            return true;
        }
    }
}
