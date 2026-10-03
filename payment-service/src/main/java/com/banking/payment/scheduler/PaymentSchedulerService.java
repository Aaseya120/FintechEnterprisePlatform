package com.banking.payment.scheduler;

import com.banking.payment.client.AccountClient;
import com.banking.payment.domain.StandingInstruction;
import com.banking.payment.domain.Transfer;
import com.banking.payment.domain.TransferStatus;
import com.banking.payment.repository.StandingInstructionRepository;
import com.banking.payment.repository.TransferRepository;
import com.banking.payment.saga.TransferSagaOrchestrator;
import com.banking.payment.service.StandingInstructionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Enterprise Payment Schedulers & Background Cron Processing:
 * 1. Automatic recovery for stalled/timed-out in-flight payments (network timeout resolution).
 * 2. Daily morning Standing Instructions (SI) & recurring auto-debit processing.
 */
@Service
public class PaymentSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(PaymentSchedulerService.class);

    private final TransferRepository transferRepository;
    private final TransferSagaOrchestrator sagaOrchestrator;
    private final StandingInstructionRepository instructionRepository;
    private final StandingInstructionService instructionService;
    private final AccountClient accountClient;

    public PaymentSchedulerService(TransferRepository transferRepository,
                                   TransferSagaOrchestrator sagaOrchestrator,
                                   StandingInstructionRepository instructionRepository,
                                   StandingInstructionService instructionService,
                                   AccountClient accountClient) {
        this.transferRepository = transferRepository;
        this.sagaOrchestrator = sagaOrchestrator;
        this.instructionRepository = instructionRepository;
        this.instructionService = instructionService;
        this.accountClient = accountClient;
    }

    /**
     * Stalled Transaction Recovery: Runs every 60 seconds.
     * Detects transactions stuck in INITIATED or DEBITED for over 5 minutes (e.g. gateway dropped connection)
     * and triggers automated Saga compensation rollback to prevent customer fund freezing.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    @Transactional
    public void recoverStalledTransfers() {
        Instant cutoff = Instant.now().minus(5, ChronoUnit.MINUTES);
        List<Transfer> stalled = transferRepository.findStalledTransfers(cutoff);

        if (stalled.isEmpty()) {
            return;
        }

        log.warn("Payment Scheduler: Detected {} stalled transfers requiring timeout recovery", stalled.size());

        for (Transfer t : stalled) {
            log.info("Executing automated timeout resolution for transferId={}, sagaId={}, status={}",
                    t.getId(), t.getSagaId(), t.getStatus());

            if (t.getStatus() == TransferStatus.DEBITED) {
                // Funds were debited from source but never credited to target: execute compensating credit
                try {
                    String compensationCorrId = "comp_recovery_" + UUID.randomUUID();
                    accountClient.credit(t.getSourceAccount(), t.getAmount(), compensationCorrId);
                    t.updateStatus(TransferStatus.COMPENSATED);
                    t.markFailed("GATEWAY_TIMEOUT: Auto-compensated by payment recovery scheduler. Funds returned to source account.");
                    transferRepository.save(t);
                    log.info("Saga compensation successful for transferId={}: credited {} {} back to source account {}",
                            t.getId(), t.getAmount(), t.getCurrency(), t.getSourceAccount());
                } catch (Exception compEx) {
                    log.error("CRITICAL: Saga compensation FAILED for transferId={}! Manual intervention required. Source account {} may have lost {} {}. Cause: {}",
                            t.getId(), t.getSourceAccount(), t.getAmount(), t.getCurrency(), compEx.getMessage());
                    t.markFailed("CRITICAL: Compensation failed during recovery — manual intervention required: " + compEx.getMessage());
                    transferRepository.save(t);
                }
            } else if (t.getStatus() == TransferStatus.INITIATED) {
                t.markFailed("TRANSACTION_EXPIRED: Abandoned before debit execution");
                transferRepository.save(t);
            }
        }
    }

    /**
     * Standing Instructions & Recurring Payments Scheduler:
     * Runs every morning at 06:00 AM (0 0 6 * * *) to execute scheduled auto-debits (rent, loan EMIs, utility bills).
     */
    @Scheduled(cron = "0 0 6 * * *")
    public void processDailyStandingInstructions() {
        log.info("Payment Scheduler: Executing 06:00 AM Standing Instructions batch run across active accounts");
        LocalDate today = LocalDate.now();
        List<StandingInstruction> dueInstructions =
                instructionRepository.findByStatusAndNextExecutionDateLessThanEqual(
                        StandingInstruction.InstructionStatus.ACTIVE, today);

        log.info("Payment Scheduler: Found {} active standing instructions due for execution on or before {}",
                dueInstructions.size(), today);

        for (StandingInstruction si : dueInstructions) {
            try {
                instructionService.executeStandingInstruction(si);
            } catch (Exception ex) {
                log.error("Failed to execute standing instruction {}: {}", si.getId(), ex.getMessage());
            }
        }
    }
}
