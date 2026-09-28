package com.enterprise.fintech.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.Objects;

@Configuration
public class RateLimiterConfig {

    /**
     * Resolves key based on authenticated user or client remote IP.
     */
    @Bean
    @Primary
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .map(p -> p.getName())
                .switchIfEmpty(Mono.just(
                        Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
                                .getAddress()
                                .getHostAddress()
                ));
    }
}
