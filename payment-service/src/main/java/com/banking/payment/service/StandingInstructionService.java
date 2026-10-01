package com.banking.payment.service;

import com.banking.common.audit.BankingServiceRegistry;
import com.banking.common.exception.BankingException;
import com.banking.payment.domain.StandingInstruction;
import com.banking.payment.dto.StandingInstructionDtos.*;
import com.banking.payment.dto.TransferRequestDto;
import com.banking.payment.repository.StandingInstructionRepository;
import com.banking.payment.saga.TransferSagaOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class StandingInstructionService {

    private static final Logger log = LoggerFactory.getLogger(StandingInstructionService.class);

    private final StandingInstructionRepository instructionRepository;
    private final TransferSagaOrchestrator sagaOrchestrator;

    public StandingInstructionService(StandingInstructionRepository instructionRepository,
                                      TransferSagaOrchestrator sagaOrchestrator) {
        this.instructionRepository = instructionRepository;
        this.sagaOrchestrator = sagaOrchestrator;
    }

    @Transactional
    public StandingInstructionResponseDto createStandingInstruction(CreateStandingInstructionRequestDto dto) {
        String id = "si_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        StandingInstruction si = new StandingInstruction(
                id,
                dto.instructionName(),
                dto.customerId(),
                dto.sourceAccountNumber(),
                dto.targetAccountNumber(),
                dto.amount(),
                dto.currency(),
                dto.frequency(),
                dto.executionDay(),
                dto.startDate(),
                dto.category()
        );

        StandingInstruction saved = instructionRepository.save(si);
        log.info("[{}] Standing Instruction created: id={}, name='{}', amount={} {}, nextRun={}",
                BankingServiceRegistry.PAYMENT_TRANSFER.getServiceId(), saved.getId(), saved.getInstructionName(),
                saved.getAmount(), saved.getCurrency(), saved.getNextExecutionDate());

        return StandingInstructionResponseDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<StandingInstructionResponseDto> getCustomerInstructions(String customerId) {
        return instructionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(StandingInstructionResponseDto::fromEntity)
                .toList();
    }

    @Transactional
    public StandingInstructionResponseDto updateStatus(String instructionId, StandingInstruction.InstructionStatus newStatus) {
        StandingInstruction si = instructionRepository.findById(instructionId)
                .orElseThrow(() -> new BankingException("INSTRUCTION_NOT_FOUND", "Standing instruction not found with ID: " + instructionId, HttpStatus.NOT_FOUND));

        si.setStatus(newStatus);
        StandingInstruction updated = instructionRepository.save(si);
        log.info("[{}] Updated Standing Instruction {} status to {}", BankingServiceRegistry.PAYMENT_TRANSFER.getServiceId(), instructionId, newStatus);
        return StandingInstructionResponseDto.fromEntity(updated);
    }

    /**
     * Executes a due standing instruction through the resilient distributed Saga Orchestrator.
     */
    @Transactional
    public void executeStandingInstruction(StandingInstruction si) {
        log.info("[{}] Executing recurring payment for SI [{} - {}]: {} {} from {} to {}",
                BankingServiceRegistry.PAYMENT_TRANSFER.getServiceId(), si.getId(), si.getInstructionName(),
                si.getAmount(), si.getCurrency(), si.getSourceAccountNumber(), si.getTargetAccountNumber());

        String idempotencyKey = "si_exec_" + si.getId() + "_" + si.getNextExecutionDate().toString();
        String correlationId = "corr_si_" + UUID.randomUUID().toString();

        TransferRequestDto request = new TransferRequestDto(
                si.getSourceAccountNumber(),
                si.getTargetAccountNumber(),
                si.getAmount(),
                si.getCurrency()
        );

        try {
            sagaOrchestrator.executeTransferSaga(request, idempotencyKey, "STANDING_INSTRUCTION", correlationId);
            si.advanceExecutionDate();
            instructionRepository.save(si);
            log.info("[{}] Recurring SI [{}] completed successfully. Next scheduled execution: {}",
                    BankingServiceRegistry.PAYMENT_TRANSFER.getServiceId(), si.getId(), si.getNextExecutionDate());
        } catch (Exception ex) {
            log.error("[{}] Recurring SI [{}] execution failed: {}", BankingServiceRegistry.PAYMENT_TRANSFER.getServiceId(), si.getId(), ex.getMessage());
            // SI remains at current execution date to retry or flag for manual review
        }
    }
}
