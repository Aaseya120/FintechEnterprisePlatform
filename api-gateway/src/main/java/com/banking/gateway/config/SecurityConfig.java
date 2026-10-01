package com.banking.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Gateway Security Configuration:
 * Token validation is handled by JwtGatewayAuthFilter (reactive WebFilter) which validates
 * HMAC-SHA256 signed JWT tokens and populates ReactiveSecurityContext.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(corsSpec -> {}) // Uses default CORS configuration from application.yml
            .authorizeExchange(exchanges -> exchanges
                // Public Actuator & Monitoring
                .pathMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                // Swagger / OpenAPI documentation
                .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/webjars/**").permitAll()
                // Authentication endpoints (login, register, refresh-token) - must be public
                .pathMatchers("/api/v1/customers/auth/**").permitAll()
                // Exchange Rates (public read-only)
                .pathMatchers(HttpMethod.GET, "/api/v1/exchange-rates/**").permitAll()
                // Admin operations
                .pathMatchers("/api/v1/admin/**").hasRole("ADMIN")
                // Core Banking & Payment operations (Customer or Teller)
                .pathMatchers("/api/v1/payments/**").hasAnyRole("CUSTOMER", "TELLER", "ADMIN")
                .pathMatchers("/api/v1/accounts/**").hasAnyRole("CUSTOMER", "TELLER", "ADMIN")
                .pathMatchers("/graphql").hasAnyRole("CUSTOMER", "TELLER", "ADMIN")
                // All other requests require authentication
                .anyExchange().authenticated()
            );

        return http.build();
    }
}
