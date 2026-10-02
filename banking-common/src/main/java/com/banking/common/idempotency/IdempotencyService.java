package com.banking.common.idempotency;

import com.banking.common.exception.BankingException;
import com.banking.common.exception.DuplicateTransactionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final String KEY_PREFIX = "idempotency:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public IdempotencyService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public String computePayloadHash(Object payload) {
        if (payload == null) {
            return "EMPTY_PAYLOAD";
        }
        try {
            String json = objectMapper.writeValueAsString(payload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new BankingException("CRYPTO_ERROR", "SHA-256 algorithm unavailable", HttpStatus.INTERNAL_SERVER_ERROR, e);
        } catch (Exception e) {
            log.warn("Failed to serialize payload for hash calculation, falling back to string hashcode: {}", e.getMessage());
            return String.valueOf(payload.hashCode());
        }
    }

    public boolean lockOrRetrieve(String idempotencyKey, String requestHash, long lockSeconds) {
        String redisKey = KEY_PREFIX + idempotencyKey;

        // Try atomic set if absent for in-flight locking
        IdempotentRecord initialRecord = IdempotentRecord.inProgress(idempotencyKey, requestHash);
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(redisKey, initialRecord, Duration.ofSeconds(lockSeconds));

        if (Boolean.TRUE.equals(acquired)) {
            log.debug("Acquired idempotency lock for key: {}", idempotencyKey);
            return true;
        }

        // If not acquired, inspect existing record
        Object existing = redisTemplate.opsForValue().get(redisKey);
        if (existing instanceof IdempotentRecord record) {
            if (!record.requestHash().equals(requestHash)) {
                log.error("Idempotency key '{}' reused with differing request payload", idempotencyKey);
                throw new BankingException("IDEMPOTENCY_MISMATCH",
                        "Idempotency key was previously used with a different request payload",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }

            if (record.status() == IdempotentRecord.IdempotencyStatus.IN_PROGRESS) {
                log.warn("Transaction with idempotency key '{}' is already in-flight", idempotencyKey);
                throw new DuplicateTransactionException(idempotencyKey);
            }
        }
        return false;
    }

    public Optional<IdempotentRecord> getRecord(String idempotencyKey) {
        String redisKey = KEY_PREFIX + idempotencyKey;
        Object obj = redisTemplate.opsForValue().get(redisKey);
        if (obj instanceof IdempotentRecord record) {
            return Optional.of(record);
        }
        return Optional.empty();
    }

    public void complete(String idempotencyKey, Object responseBody, int statusCode, long ttl, TimeUnit unit) {
        String redisKey = KEY_PREFIX + idempotencyKey;
        Optional<IdempotentRecord> current = getRecord(idempotencyKey);
        if (current.isPresent()) {
            IdempotentRecord completed = current.get().complete(responseBody, statusCode);
            redisTemplate.opsForValue().set(redisKey, completed, ttl, unit);
            log.info("Idempotency record completed and cached [key: {}, ttl: {} {}]", idempotencyKey, ttl, unit);
        }
    }

    public void releaseLock(String idempotencyKey) {
        String redisKey = KEY_PREFIX + idempotencyKey;
        redisTemplate.delete(redisKey);
        log.debug("Released idempotency lock for key: {}", idempotencyKey);
    }
}
