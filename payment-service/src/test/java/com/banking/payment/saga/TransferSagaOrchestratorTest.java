package com.banking.payment.saga;

import com.banking.common.exception.BankingException;
import com.banking.payment.client.AccountClient;
import com.banking.payment.domain.OutboxEventEntity;
import com.banking.payment.domain.Transfer;
import com.banking.payment.domain.TransferStatus;
import com.banking.payment.dto.TransferRequestDto;
import com.banking.payment.dto.TransferResponseDto;
import com.banking.payment.repository.OutboxEventRepository;
import com.banking.payment.repository.TransferRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Transfer Saga Orchestration & Distributed Compensation Tests")
class TransferSagaOrchestratorTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private AccountClient accountClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TransferSagaOrchestrator sagaOrchestrator;

    private TransferRequestDto request;

    @BeforeEach
    void setUp() {
        request = new TransferRequestDto(
                "US1111111111",
                "US2222222222",
                new BigDecimal("500.00"),
                "USD"
        );
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Should successfully execute 2-phase Saga: debit source, credit target, complete transfer")
    void shouldExecuteTransferSagaSuccessfully() {
        when(accountClient.debit(eq("US1111111111"), eq(new BigDecimal("500.00")), any())).thenReturn(true);
        when(accountClient.credit(eq("US2222222222"), eq(new BigDecimal("500.00")), any())).thenReturn(true);

        TransferResponseDto result = sagaOrchestrator.executeTransferSaga(
                request,
                "idemp-key-101",
                "IOS",
                "corr-abc"
        );

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(result.channel()).isEqualTo("IOS");

        // Verify outbox events were recorded for reliable Kafka streaming
        verify(outboxEventRepository, atLeast(2)).save(any(OutboxEventEntity.class));
        verify(accountClient, times(1)).debit(any(), any(), any());
        verify(accountClient, times(1)).credit(any(), any(), any());
    }

    @Test
    @DisplayName("Should initiate Saga compensation when credit phase fails after debit succeeds")
    void shouldCompensateWhenTargetCreditFails() {
        // Step 1: Debit succeeds
        when(accountClient.debit(eq("US1111111111"), eq(new BigDecimal("500.00")), any())).thenReturn(true);
        // Step 2: Credit fails
        when(accountClient.credit(eq("US2222222222"), eq(new BigDecimal("500.00")), any()))
                .thenThrow(new BankingException("ACCOUNT_SERVICE_UNAVAILABLE", "Target bank unavailable", HttpStatus.SERVICE_UNAVAILABLE));
        // Compensation: Credit back to source succeeds
        when(accountClient.credit(eq("US1111111111"), eq(new BigDecimal("500.00")), any())).thenReturn(true);

        assertThatThrownBy(() -> sagaOrchestrator.executeTransferSaga(
                request,
                "idemp-key-202",
                "ANDROID",
                "corr-xyz"
        ))
        .isInstanceOf(BankingException.class)
        .hasMessageContaining("Transfer could not complete and was reversed back to source account");

        // Verify compensation occurred
        verify(accountClient, times(1)).credit(eq("US1111111111"), eq(new BigDecimal("500.00")), any());
        // Verify outbox recorded the compensation event
        verify(outboxEventRepository, atLeast(3)).save(any(OutboxEventEntity.class));
    }
}
