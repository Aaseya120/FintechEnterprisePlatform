package com.banking.loan.service;

import com.banking.common.exception.BankingException;
import com.banking.common.exception.ResourceNotFoundException;
import com.banking.loan.domain.Loan;
import com.banking.loan.domain.LoanRepaymentSchedule;
import com.banking.loan.dto.LoanDtos.*;
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
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public LoanService(LoanRepository loanRepository,
                       LoanRepaymentScheduleRepository scheduleRepository,
                       KafkaTemplate<String, Object> kafkaTemplate) {
        this.loanRepository = loanRepository;
        this.scheduleRepository = scheduleRepository;
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
        kafkaTemplate.send("banking.loan.disbursed", saved.getId(), saved);
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
