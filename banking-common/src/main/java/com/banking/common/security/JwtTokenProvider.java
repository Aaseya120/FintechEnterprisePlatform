package com.banking.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

/**
 * Enterprise JWT Access Token & Refresh Token Provider:
 * Generates and validates signed HMAC-SHA256 JWT tokens with role, tier, and session claims.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String secretKey;
    private final long accessTokenValiditySeconds;
    private final long refreshTokenValidityDays;
    private final ObjectMapper objectMapper;

    public JwtTokenProvider(
            @Value("${banking.security.jwt.secret}") String secretKey,
            @Value("${banking.security.jwt.access-token-validity-seconds:900}") long accessTokenValiditySeconds,
            @Value("${banking.security.jwt.refresh-token-validity-days:7}") long refreshTokenValidityDays,
            ObjectMapper objectMapper) {
        this.secretKey = secretKey;
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
        this.refreshTokenValidityDays = refreshTokenValidityDays;
        this.objectMapper = objectMapper;
    }

    /**
     * Generates a 15-minute validity signed JWT Access Token.
     */
    public String generateAccessToken(String customerId, String customerNumber, String email,
                                      String tier, List<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenValiditySeconds);

        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", customerId);
        claims.put("customerNumber", customerNumber);
        claims.put("email", email);
        claims.put("tier", tier);
        claims.put("roles", roles);
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", expiry.getEpochSecond());
        claims.put("jti", UUID.randomUUID().toString());

        try {
            String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(header));
            String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(claims));
            String dataToSign = encodedHeader + "." + encodedPayload;
            String signature = sign(dataToSign);
            return dataToSign + "." + signature;
        } catch (Exception e) {
            log.error("Failed to generate JWT access token: {}", e.getMessage());
            throw new RuntimeException("Error signing JWT access token", e);
        }
    }

    /**
     * Generates a high-entropy opaque Refresh Token.
     */
    public String generateRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return "rt_" + HexFormat.of().formatHex(bytes);
    }

    public long getAccessTokenValiditySeconds() {
        return accessTokenValiditySeconds;
    }

    public long getRefreshTokenValidityDays() {
        return refreshTokenValidityDays;
    }

    /**
     * Validates JWT token structure, HMAC signature, and expiration.
     */
    public boolean validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;

            String dataToSign = parts[0] + "." + parts[1];
            String expectedSignature = sign(dataToSign);
            if (!MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8))) {
                return false;
            }

            // Check expiration
            Map<String, Object> claims = getClaims(token);
            long exp = ((Number) claims.get("exp")).longValue();
            return Instant.now().getEpochSecond() < exp;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getClaims(String token) {
        try {
            String[] parts = token.split("\\.");
            byte[] decodedBytes = Base64.getUrlDecoder().decode(parts[1]);
            return objectMapper.readValue(decodedBytes, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Invalid JWT token format", e);
        }
    }

    public String getSubject(String token) {
        return (String) getClaims(token).get("sub");
    }

    public String getCustomerTier(String token) {
        return (String) getClaims(token).get("tier");
    }

    private String sign(String data) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmac.init(secretKeySpec);
        byte[] signedBytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signedBytes);
    }
}
