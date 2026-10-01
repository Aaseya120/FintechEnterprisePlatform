package com.banking.gateway.ratelimit;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Optional;

@Configuration
public class RateLimiterConfig {

    /**
     * Resolves the rate-limiting key:
     * 1. Authenticated user ID (JWT subject) if present.
     * 2. Fallbacks to client IP address for unauthenticated requests.
     */
    @Bean
    @Primary
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .cast(Authentication.class)
                .map(auth -> {
                    if (auth.getPrincipal() instanceof Jwt jwt) {
                        return "usr:" + jwt.getSubject();
                    }
                    return "usr:" + auth.getName();
                })
                .switchIfEmpty(Mono.fromSupplier(() -> {
                    InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
                    if (remoteAddress != null && remoteAddress.getAddress() != null) {
                        return "ip:" + remoteAddress.getAddress().getHostAddress();
                    }
                    return "ip:anonymous";
                }));
    }

    /**
     * Standard Banking Tier: 50 requests/sec replenish rate, burst capacity of 100 requests.
     */
    @Bean
    public RedisRateLimiter standardRateLimiter() {
        return new RedisRateLimiter(50, 100, 1);
    }

    /**
     * Critical Payment Transfer Tier: 10 requests/sec replenish rate, burst capacity of 20 requests
     * to protect against rapid-fire duplicate fund transfer attempts.
     */
    @Bean
    public RedisRateLimiter paymentRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }
}
