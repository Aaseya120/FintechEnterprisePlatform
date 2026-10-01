package com.banking.common.idempotency;

import com.banking.common.exception.BankingException;
import com.banking.common.exception.DuplicateTransactionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Distributed Idempotency Engine Tests")
@SuppressWarnings("null")
class IdempotencyServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private IdempotencyService idempotencyService;

    private final String testKey = "idemp-test-uuid";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Should successfully acquire lock on initial transaction request")
    void shouldAcquireLockOnInitialRequest() {
        when(valueOperations.setIfAbsent(eq("idempotency:" + testKey), any(IdempotentRecord.class), any(Duration.class)))
                .thenReturn(true);

        boolean locked = idempotencyService.lockOrRetrieve(testKey, "hash123", 60);
        assertThat(locked).isTrue();
    }

    @Test
    @DisplayName("Should throw DuplicateTransactionException if transaction is already in-flight")
    void shouldThrowWhenTransactionInFlight() {
        when(valueOperations.setIfAbsent(anyString(), any(), any())).thenReturn(false);
        IdempotentRecord inProgress = IdempotentRecord.inProgress(testKey, "hash123");
        when(valueOperations.get("idempotency:" + testKey)).thenReturn(inProgress);

        assertThatThrownBy(() -> idempotencyService.lockOrRetrieve(testKey, "hash123", 60))
                .isInstanceOf(DuplicateTransactionException.class);
    }

    @Test
    @DisplayName("Should throw BankingException when idempotency key is reused with different payload")
    void shouldThrowWhenPayloadMismatch() {
        when(valueOperations.setIfAbsent(anyString(), any(), any())).thenReturn(false);
        IdempotentRecord existing = new IdempotentRecord(testKey, "hash_original",
                IdempotentRecord.IdempotencyStatus.COMPLETED, Map.of("status", "ok"), 200, Instant.now());
        when(valueOperations.get("idempotency:" + testKey)).thenReturn(existing);

        assertThatThrownBy(() -> idempotencyService.lockOrRetrieve(testKey, "hash_tampered_new_amount", 60))
                .isInstanceOf(BankingException.class)
                .hasMessageContaining("different request payload");
    }

    @Test
    @DisplayName("Should retrieve completed cached record")
    void shouldRetrieveCachedCompletedRecord() {
        IdempotentRecord completed = new IdempotentRecord(testKey, "hash123",
                IdempotentRecord.IdempotencyStatus.COMPLETED, Map.of("transferId", "tx_999"), 200, Instant.now());
        when(valueOperations.get("idempotency:" + testKey)).thenReturn(completed);

        Optional<IdempotentRecord> result = idempotencyService.getRecord(testKey);
        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo(IdempotentRecord.IdempotencyStatus.COMPLETED);
    }
}
