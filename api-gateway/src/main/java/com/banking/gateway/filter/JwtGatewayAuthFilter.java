package com.banking.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reactive JWT Authentication WebFilter for Spring Cloud Gateway:
 * Validates the 'Authorization: Bearer <token>' header using HMAC-SHA256 signature verification,
 * then populates the ReactiveSecurityContext with the authenticated user principal and roles.
 */
@Component
public class JwtGatewayAuthFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtGatewayAuthFilter.class);

    private final String secretKey;
    private final ObjectMapper objectMapper;

    public JwtGatewayAuthFilter(
            @Value("${banking.security.jwt.secret:EnterpriseBankingSuperSecretKeyMustBeAtLeast256BitsLongForHmacSha256Security2026!}") String secretKey,
            ObjectMapper objectMapper) {
        this.secretKey = secretKey;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String bearerToken = extractBearerToken(exchange);

        if (bearerToken == null) {
            return chain.filter(exchange);
        }

        try {
            String[] parts = bearerToken.split("\\.");
            if (parts.length != 3) {
                return chain.filter(exchange);
            }

            // Verify HMAC-SHA256 signature using constant-time comparison
            String dataToSign = parts[0] + "." + parts[1];
            String expectedSignature = sign(dataToSign);
            if (!MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8))) {
                log.warn("JWT signature verification failed for request: {}", exchange.getRequest().getPath());
                return rejectUnauthorized(exchange, "Invalid token signature");
            }

            // Decode claims and check expiration
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = objectMapper.readValue(
                    Base64.getUrlDecoder().decode(parts[1]), Map.class);

            long exp = ((Number) claims.get("exp")).longValue();
            if (Instant.now().getEpochSecond() >= exp) {
                log.debug("JWT token expired for request: {}", exchange.getRequest().getPath());
                return rejectUnauthorized(exchange, "Token expired");
            }

            // Extract user identity and roles
            String customerId = (String) claims.get("sub");

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.getOrDefault("roles", Collections.emptyList());
            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> role.startsWith("ROLE_") ? new SimpleGrantedAuthority(role) : new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(customerId, null, authorities);

            // Propagate customerId and roles downstream as headers for tracing & RBAC
            String rolesHeader = String.join(",", roles);
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(r -> r.header("X-Authenticated-User", customerId)
                                   .header("X-Authenticated-Roles", rolesHeader))
                    .build();

            return chain.filter(mutatedExchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));

        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
            return chain.filter(exchange);
        }
    }

    private String extractBearerToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private String sign(String data) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmac.init(keySpec);
        byte[] signedBytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signedBytes);
    }

    private Mono<Void> rejectUnauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().set("WWW-Authenticate",
                "Bearer error=\"invalid_token\", error_description=\"" + message + "\"");
        return exchange.getResponse().setComplete();
    }
}
