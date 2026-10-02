package com.banking.loan.service;

import com.banking.common.exception.BankingException;
import com.banking.common.exception.ResourceNotFoundException;
import com.banking.loan.domain.Loan;
import com.banking.loan.domain.LoanRepayment;
import com.banking.loan.domain.LoanRepaymentSchedule;
import com.banking.loan.dto.LoanDtos.*;
import com.banking.loan.repository.LoanRepaymentRepository;
import com.banking.loan.repository.LoanRepaymentScheduleRepository;
import com.banking.loan.repository.LoanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LoanService {

    private static final Logger log = LoggerFactory.getLogger(LoanService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final LoanRepository loanRepository;
    private final LoanRepaymentScheduleRepository scheduleRepository;
    private final LoanRepaymentRepository repaymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public LoanService(LoanRepository loanRepository,
                       LoanRepaymentScheduleRepository scheduleRepository,
                       LoanRepaymentRepository repaymentRepository,
                       KafkaTemplate<String, Object> kafkaTemplate) {
        this.loanRepository = loanRepository;
        this.scheduleRepository = scheduleRepository;
        this.repaymentRepository = repaymentRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public EmiCalculationResponseDto calculateEmi(EmiCalculationRequestDto req) {
        BigDecimal emi = computeMonthlyEmi(req.principalAmount(), req.annualInterestRate(), req.tenureMonths());
        BigDecimal totalPayment = emi.multiply(BigDecimal.valueOf(req.tenureMonths())).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalInterest = totalPayment.subtract(req.principalAmount()).setScale(2, RoundingMode.HALF_UP);

        return new EmiCalculationResponseDto(
                req.principalAmount(),
                req.annualInterestRate(),
                req.tenureMonths(),
                emi,
                totalInterest,
                totalPayment
        );
    }

    @Transactional
    public LoanResponseDto applyForLoan(LoanApplicationRequestDto req) {
        BigDecimal emi = computeMonthlyEmi(req.principalAmount(), req.annualInterestRate(), req.tenureMonths());
        String loanAccountNum = "LN" + (1000000000L + (long)(RANDOM.nextDouble() * 9000000000L));

        Loan loan = new Loan(
                UUID.randomUUID().toString(),
                loanAccountNum,
                req.customerId(),
                req.loanType(),
                req.principalAmount(),
                req.annualInterestRate(),
                req.tenureMonths(),
                emi,
                req.disbursementAccount()
        );

        Loan saved = loanRepository.save(loan);
        log.info("Loan applied [Account: {}, Type: {}, Principal: {}]", loanAccountNum, req.loanType(), req.principalAmount());
        return mapToDto(saved);
    }

    @Transactional
    public LoanResponseDto approveLoan(String loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));

        if (loan.getStatus() != Loan.LoanStatus.APPLIED) {
            throw new BankingException("INVALID_LOAN_STATUS", "Loan must be in APPLIED status for approval", HttpStatus.BAD_REQUEST);
        }

        loan.approve();
        Loan saved = loanRepository.save(loan);
        log.info("Loan approved: {}", loan.getLoanAccountNumber());
        return mapToDto(saved);
    }

    @Transactional
    public LoanResponseDto disburseLoan(String loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));

        if (loan.getStatus() != Loan.LoanStatus.APPROVED) {
            throw new BankingException("LOAN_NOT_APPROVED", "Cannot disburse unapproved loan", HttpStatus.BAD_REQUEST);
        }

        loan.disburse();
        Loan saved = loanRepository.save(loan);

        // Generate Amortization Schedule
        generateAmortizationSchedule(saved);

        // Publish Loan Disbursed Event for Account Service to Credit Customer's Account
        kafkaTemplate.send("banking.loan.disbursed", saved.getId(), saved)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish loan.disbursed event for loan {}: {}", saved.getId(), ex.getMessage());
                    }
                });
        log.info("Loan disbursed: {}. Generated amortization schedule.", saved.getLoanAccountNumber());

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<RepaymentScheduleItemDto> getSchedule(String loanId) {
        return scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId)
                .stream()
                .map(s -> new RepaymentScheduleItemDto(
                        s.getInstallmentNumber(),
                        s.getDueDate(),
                        s.getPrincipalComponent(),
                        s.getInterestComponent(),
                        s.getTotalInstallment(),
                        s.getRemainingBalance(),
                        s.getStatus().name()
                ))
                .collect(Collectors.toList());
    }

    private void generateAmortizationSchedule(Loan loan) {
        BigDecimal balance = loan.getPrincipalAmount();
        BigDecimal monthlyRate = loan.getAnnualInterestRate()
                .divide(BigDecimal.valueOf(1200), 8, RoundingMode.HALF_UP);
        BigDecimal emi = loan.getEmiAmount();
        LocalDate dueDate = LocalDate.now().plusMonths(1);

        List<LoanRepaymentSchedule> schedules = new ArrayList<>();
        for (int i = 1; i <= loan.getTenureMonths(); i++) {
            BigDecimal interestComp = balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalComp = emi.subtract(interestComp).setScale(2, RoundingMode.HALF_UP);

            if (i == loan.getTenureMonths() || principalComp.compareTo(balance) > 0) {
                principalComp = balance;
                emi = principalComp.add(interestComp);
                balance = BigDecimal.ZERO;
            } else {
                balance = balance.subtract(principalComp).setScale(2, RoundingMode.HALF_UP);
            }

            LoanRepaymentSchedule item = new LoanRepaymentSchedule(
                    UUID.randomUUID().toString(),
                    loan.getId(),
                    i,
                    dueDate,
                    principalComp,
                    interestComp,
                    emi,
                    balance
            );
            schedules.add(item);
            dueDate = dueDate.plusMonths(1);
        }
        scheduleRepository.saveAll(schedules);
    }

    @Transactional
    public LoanRepaymentResponseDto payInstallment(String loanId, LoanRepaymentRequestDto req) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));

        if (loan.getStatus() != Loan.LoanStatus.DISBURSED) {
            throw new BankingException("LOAN_NOT_ACTIVE", "Can only make repayments on active disbursed loans", HttpStatus.BAD_REQUEST);
        }

        List<LoanRepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);
        LoanRepaymentSchedule nextPending = schedules.stream()
                .filter(s -> s.getStatus() == LoanRepaymentSchedule.ScheduleStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new BankingException("NO_PENDING_SCHEDULE", "No pending installments found for this loan", HttpStatus.BAD_REQUEST));

        nextPending.setStatus(LoanRepaymentSchedule.ScheduleStatus.PAID);
        scheduleRepository.save(nextPending);

        String ref = req.transactionReference() != null ? req.transactionReference() : "REPAY_" + UUID.randomUUID().toString().substring(0, 10);
        LoanRepayment repayment = new LoanRepayment(
                UUID.randomUUID().toString(),
                loanId,
                loan.getCustomerId(),
                req.amount(),
                LoanRepayment.PaymentType.EMI_INSTALLMENT,
                req.paymentMethod(),
                ref
        );
        LoanRepayment saved = repaymentRepository.save(repayment);

        // Check if all installments are now paid
        boolean anyRemaining = schedules.stream().anyMatch(s -> s.getStatus() == LoanRepaymentSchedule.ScheduleStatus.PENDING);
        if (!anyRemaining) {
            loan.setStatus(Loan.LoanStatus.CLOSED);
            loanRepository.save(loan);
            log.info("Loan {} has been FULLY PAID OFF and CLOSED", loan.getLoanAccountNumber());
        }

        log.info("Recorded EMI installment {} payment of {} for loan {}", nextPending.getInstallmentNumber(), req.amount(), loan.getLoanAccountNumber());
        return new LoanRepaymentResponseDto(
                saved.getId(),
                saved.getLoanId(),
                saved.getCustomerId(),
                saved.getAmountPaid(),
                saved.getPaymentType(),
                saved.getPaymentMethod(),
                saved.getTransactionReference(),
                nextPending.getRemainingBalance(),
                saved.getPaidAt()
        );
    }

    @Transactional(readOnly = true)
    public LoanForeclosureQuoteDto getForeclosureQuote(String loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));

        if (loan.getStatus() != Loan.LoanStatus.DISBURSED) {
            throw new BankingException("LOAN_NOT_ACTIVE", "Loan is not active", HttpStatus.BAD_REQUEST);
        }

        List<LoanRepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);
        BigDecimal outstandingPrincipal = schedules.stream()
                .filter(s -> s.getStatus() == LoanRepaymentSchedule.ScheduleStatus.PENDING)
                .map(LoanRepaymentSchedule::getPrincipalComponent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal foreclosurePenaltyRate = new BigDecimal("0.02"); // 2% foreclosure penalty
        BigDecimal foreclosurePenalty = outstandingPrincipal.multiply(foreclosurePenaltyRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal accruedInterest = outstandingPrincipal.multiply(loan.getAnnualInterestRate())
                .divide(BigDecimal.valueOf(1200), 2, RoundingMode.HALF_UP);
        BigDecimal totalPayoff = outstandingPrincipal.add(foreclosurePenalty).add(accruedInterest);

        return new LoanForeclosureQuoteDto(
                loan.getId(),
                loan.getLoanAccountNumber(),
                outstandingPrincipal,
                accruedInterest,
                foreclosurePenalty,
                totalPayoff,
                LocalDate.now().plusDays(7)
        );
    }

    @Transactional
    public LoanRepaymentResponseDto forecloseLoan(String loanId, LoanRepaymentRequestDto req) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));

        LoanForeclosureQuoteDto quote = getForeclosureQuote(loanId);
        if (req.amount().compareTo(quote.totalPayoffAmount()) < 0) {
            throw new BankingException("INSUFFICIENT_PAYOFF_AMOUNT",
                    "Foreclosure payoff requires exact or greater amount of " + quote.totalPayoffAmount(), HttpStatus.BAD_REQUEST);
        }

        // Mark all remaining installments as PAID
        List<LoanRepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);
        for (LoanRepaymentSchedule s : schedules) {
            if (s.getStatus() == LoanRepaymentSchedule.ScheduleStatus.PENDING) {
                s.setStatus(LoanRepaymentSchedule.ScheduleStatus.PAID);
            }
        }
        scheduleRepository.saveAll(schedules);

        loan.setStatus(Loan.LoanStatus.CLOSED);
        loanRepository.save(loan);

        String ref = req.transactionReference() != null ? req.transactionReference() : "FORECLOSE_" + UUID.randomUUID().toString().substring(0, 10);
        LoanRepayment repayment = new LoanRepayment(
                UUID.randomUUID().toString(),
                loanId,
                loan.getCustomerId(),
                req.amount(),
                LoanRepayment.PaymentType.FORECLOSURE_PAYOFF,
                req.paymentMethod(),
                ref
        );
        LoanRepayment saved = repaymentRepository.save(repayment);
        log.info("Loan {} successfully FORECLOSED and CLOSED with payoff of {}", loan.getLoanAccountNumber(), req.amount());

        return new LoanRepaymentResponseDto(
                saved.getId(),
                saved.getLoanId(),
                saved.getCustomerId(),
                saved.getAmountPaid(),
                saved.getPaymentType(),
                saved.getPaymentMethod(),
                saved.getTransactionReference(),
                BigDecimal.ZERO,
                saved.getPaidAt()
        );
    }

    @Transactional(readOnly = true)
    public List<LoanRepaymentResponseDto> getRepayments(String loanId) {
        return repaymentRepository.findByLoanIdOrderByPaidAtDesc(loanId)
                .stream()
                .map(r -> new LoanRepaymentResponseDto(
                        r.getId(),
                        r.getLoanId(),
                        r.getCustomerId(),
                        r.getAmountPaid(),
                        r.getPaymentType(),
                        r.getPaymentMethod(),
                        r.getTransactionReference(),
                        BigDecimal.ZERO,
                        r.getPaidAt()
                ))
                .toList();
    }

    private BigDecimal computeMonthlyEmi(BigDecimal principal, BigDecimal annualRate, int months) {
        double p = principal.doubleValue();
        double r = annualRate.doubleValue() / (12 * 100);
        double emi = (p * r * Math.pow(1 + r, months)) / (Math.pow(1 + r, months) - 1);
        return BigDecimal.valueOf(emi).setScale(2, RoundingMode.HALF_UP);
    }

    private LoanResponseDto mapToDto(Loan l) {
        return new LoanResponseDto(
                l.getId(),
                l.getLoanAccountNumber(),
                l.getCustomerId(),
                l.getLoanType(),
                l.getPrincipalAmount(),
                l.getAnnualInterestRate(),
                l.getTenureMonths(),
                l.getEmiAmount(),
                l.getStatus(),
                l.getDisbursementAccount(),
                l.getCreatedAt()
        );
    }
}
