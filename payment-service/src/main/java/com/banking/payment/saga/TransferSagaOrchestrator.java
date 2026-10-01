package com.banking.payment.saga;

import com.banking.common.events.AccountDebitedEvent;
import com.banking.common.events.TransactionCompensatedEvent;
import com.banking.common.events.TransactionFailedEvent;
import com.banking.common.events.TransactionInitiatedEvent;
import com.banking.common.exception.BankingException;
import com.banking.payment.client.AccountClient;
import com.banking.payment.config.KafkaConfig;
import com.banking.payment.domain.OutboxEventEntity;
import com.banking.payment.domain.Transfer;
import com.banking.payment.domain.TransferStatus;
import com.banking.payment.dto.TransferRequestDto;
import com.banking.payment.dto.TransferResponseDto;
import com.banking.payment.repository.OutboxEventRepository;
import com.banking.payment.repository.TransferRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class TransferSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TransferSagaOrchestrator.class);

    private final TransferRepository transferRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AccountClient accountClient;
    private final ObjectMapper objectMapper;

    public TransferSagaOrchestrator(TransferRepository transferRepository,
                                    OutboxEventRepository outboxEventRepository,
                                    AccountClient accountClient,
                                    ObjectMapper objectMapper) {
        this.transferRepository = transferRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.accountClient = accountClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes the Saga Orchestration Workflow:
     * 1. Persistence & Transactional Outbox (Atomicity)
     * 2. Step 1: Debit Source Account (Fault-tolerant via Resilience4j)
     * 3. Step 2: Credit Target Account
     * 4. Compensating Action: Reverse Debit if Credit fails
     */
    @Transactional
    public TransferResponseDto executeTransferSaga(TransferRequestDto request, String idempotencyKey,
                                                   String channel, String correlationId) {
        String transferId = "tx_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String sagaId = "saga_" + UUID.randomUUID().toString();

        log.info("Starting Saga [{}] for transfer: {} -> {} amount: {} {} [corr: {}]",
                sagaId, request.sourceAccountNumber(), request.targetAccountNumber(),
                request.amount(), request.currency(), correlationId);

        // 1. Persist Initial Transfer Record
        Transfer transfer = new Transfer(
                transferId,
                sagaId,
                idempotencyKey,
                request.sourceAccountNumber(),
                request.targetAccountNumber(),
                request.amount(),
                request.currency(),
                channel
        );
        transfer = transferRepository.save(transfer);

        // 2. Write TransactionInitiatedEvent into Transactional Outbox
        TransactionInitiatedEvent initiatedEvent = new TransactionInitiatedEvent(
                sagaId,
                transferId,
                request.sourceAccountNumber(),
                request.targetAccountNumber(),
                request.amount(),
                request.currency(),
                idempotencyKey,
                channel,
                Instant.now()
        );
        saveOutboxEvent("Transfer", transferId, "TRANSACTION_INITIATED",
                KafkaConfig.TOPIC_TRANSACTIONS_INITIATED, initiatedEvent);

        // 3. Step 1: Debit Source Account
        boolean debited = false;
        try {
            debited = accountClient.debit(request.sourceAccountNumber(), request.amount(), correlationId);
            transfer.updateStatus(TransferStatus.DEBITED);
            transferRepository.save(transfer);
            log.info("Saga [{}]: Source account successfully debited.", sagaId);
        } catch (Exception ex) {
            log.error("Saga [{}]: Source debit failed: {}", sagaId, ex.getMessage());
            transfer.markFailed("Source debit failed: " + ex.getMessage());
            transferRepository.save(transfer);

            TransactionFailedEvent failedEvent = new TransactionFailedEvent(
                    sagaId, transferId, request.sourceAccountNumber(), request.amount(), ex.getMessage(), Instant.now()
            );
            saveOutboxEvent("Transfer", transferId, "TRANSACTION_FAILED", KafkaConfig.TOPIC_TRANSACTIONS_FAILED, failedEvent);

            if (ex instanceof BankingException be) {
                throw be;
            }
            throw new BankingException("DEBIT_FAILED", "Failed to debit source account: " + ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY, ex);
        }

        // 4. Step 2: Credit Target Account
        try {
            accountClient.credit(request.targetAccountNumber(), request.amount(), correlationId);
            transfer.updateStatus(TransferStatus.COMPLETED);
            transfer = transferRepository.save(transfer);
            log.info("Saga [{}] successfully completed fund transfer.", sagaId);

            saveOutboxEvent("Transfer", transferId, "TRANSACTION_COMPLETED",
                    KafkaConfig.TOPIC_TRANSACTIONS_COMPLETED, initiatedEvent);

        } catch (Exception ex) {
            log.error("Saga [{}]: Credit to target failed! Initiating compensation: {}", sagaId, ex.getMessage());
            transfer.updateStatus(TransferStatus.COMPENSATING);
            transferRepository.save(transfer);

            // Execute Compensation: Re-credit source account
            try {
                accountClient.credit(request.sourceAccountNumber(), request.amount(), correlationId);
                transfer.updateStatus(TransferStatus.COMPENSATED);
                transfer.markFailed("Credit failed, compensated: " + ex.getMessage());
                transfer = transferRepository.save(transfer);

                TransactionCompensatedEvent compEvent = new TransactionCompensatedEvent(
                        sagaId, transferId, request.sourceAccountNumber(), request.amount(),
                        "Target credit failed: " + ex.getMessage(), Instant.now()
                );
                saveOutboxEvent("Transfer", transferId, "TRANSACTION_COMPENSATED",
                        KafkaConfig.TOPIC_TRANSACTIONS_FAILED, compEvent);

                log.warn("Saga [{}] compensated successfully.", sagaId);
            } catch (Exception compEx) {
                log.error("CRITICAL: Saga [{}] compensation failed! Manual intervention required: {}", sagaId, compEx.getMessage());
                transfer.markFailed("CRITICAL: Compensation failed: " + compEx.getMessage());
                transferRepository.save(transfer);
            }

            throw new BankingException("TRANSFER_FAILED_COMPENSATED",
                    "Transfer could not complete and was reversed back to source account.",
                    HttpStatus.UNPROCESSABLE_ENTITY, ex);
        }

        return mapToDto(transfer);
    }

    private void saveOutboxEvent(String aggregateType, String aggregateId, String eventType, String topic, Object event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            OutboxEventEntity outbox = new OutboxEventEntity(
                    UUID.randomUUID().toString(),
                    aggregateType,
                    aggregateId,
                    eventType,
                    topic,
                    json
            );
            outboxEventRepository.save(outbox);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event {}: {}", eventType, e.getMessage());
        }
    }

    public TransferResponseDto mapToDto(Transfer transfer) {
        return new TransferResponseDto(
                transfer.getId(),
                transfer.getSagaId(),
                transfer.getIdempotencyKey(),
                transfer.getSourceAccount(),
                transfer.getTargetAccount(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getStatus(),
                transfer.getFailureReason(),
                transfer.getChannel(),
                transfer.getCreatedAt(),
                transfer.getUpdatedAt()
        );
    }
}
