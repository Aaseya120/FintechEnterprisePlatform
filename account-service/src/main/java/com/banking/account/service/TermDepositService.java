package com.banking.account.service;

import com.banking.account.domain.Account;
import com.banking.account.domain.TermDeposit;
import com.banking.account.dto.TermDepositDtos.*;
import com.banking.account.repository.AccountRepository;
import com.banking.account.repository.TermDepositRepository;
import com.banking.common.exception.BankingException;
import com.banking.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TermDepositService {

    private static final Logger log = LoggerFactory.getLogger(TermDepositService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TermDepositRepository termDepositRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;

    public TermDepositService(TermDepositRepository termDepositRepository,
                              AccountRepository accountRepository,
                              AccountService accountService) {
        this.termDepositRepository = termDepositRepository;
        this.accountRepository = accountRepository;
        this.accountService = accountService;
    }

    @Transactional
    public TermDepositResponse openTermDeposit(OpenTermDepositRequest request) {
        Account linkedAccount = accountRepository.findByAccountNumber(request.linkedAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Linked Account", request.linkedAccountNumber()));

        if (linkedAccount.getAvailableBalance().compareTo(request.principalAmount()) < 0) {
            throw new BankingException("INSUFFICIENT_FUNDS", "Insufficient balance in linked account to fund Term Deposit", HttpStatus.BAD_REQUEST);
        }

        // Debit linked checking/savings account
        accountService.debitAccount(request.linkedAccountNumber(), request.principalAmount(),
                UUID.randomUUID().toString(), "TERM_DEPOSIT_CREATION");

        // Determine interest rate based on tenor
        BigDecimal interestRate = getInterestRateForTenor(request.tenorMonths());
        String depositNumber = "FD-" + (10000000 + RANDOM.nextInt(90000000));

        TermDeposit td = new TermDeposit(
                UUID.randomUUID().toString(),
                depositNumber,
                request.customerId(),
                request.linkedAccountNumber(),
                request.principalAmount(),
                request.currency(),
                interestRate,
                request.tenorMonths(),
                request.compoundingFrequency() != null ? request.compoundingFrequency() : TermDeposit.CompoundingFrequency.QUARTERLY,
                request.autoRenewal()
        );

        TermDeposit saved = termDepositRepository.save(td);
        log.info("Opened new Term Deposit {} for customer {} - Principal: {}, Maturity: {}",
                depositNumber, request.customerId(), request.principalAmount(), saved.getMaturityAmount());

        return mapToDto(saved);
    }

    @Transactional
    public TermDepositLiquidationResponse liquidateDeposit(String depositNumber) {
        TermDeposit td = termDepositRepository.findByDepositNumber(depositNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Term Deposit", depositNumber));

        if (td.getStatus() != TermDeposit.TermDepositStatus.ACTIVE) {
            throw new BankingException("ALREADY_CLOSED", "Term Deposit is not active", HttpStatus.CONFLICT);
        }

        // Premature liquidation calculation: 1% penalty deducted from interest
        BigDecimal penalty = td.getPrincipalAmount().multiply(new BigDecimal("0.01")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal interestAccrued = td.getPrincipalAmount().multiply(td.getInterestRate()).multiply(new BigDecimal("0.25")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netPayout = td.getPrincipalAmount().add(interestAccrued).subtract(penalty);

        td.setStatus(TermDeposit.TermDepositStatus.PREMATURE_CLOSED);
        termDepositRepository.save(td);

        // Credit proceeds back to linked checking/savings account
        accountService.creditAccount(td.getLinkedAccountNumber(), netPayout,
                UUID.randomUUID().toString(), "TERM_DEPOSIT_LIQUIDATION");

        log.info("Liquidated Term Deposit {} - Net Payout {} returned to {}",
                depositNumber, netPayout, td.getLinkedAccountNumber());

        return new TermDepositLiquidationResponse(
                depositNumber,
                td.getPrincipalAmount(),
                interestAccrued,
                penalty,
                netPayout,
                td.getLinkedAccountNumber(),
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public List<TermDepositResponse> getCustomerDeposits(String customerId) {
        return termDepositRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private BigDecimal getInterestRateForTenor(int months) {
        if (months < 6) return new BigDecimal("0.0550"); // 5.50%
        if (months < 12) return new BigDecimal("0.0650"); // 6.50%
        if (months < 36) return new BigDecimal("0.0725"); // 7.25%
        return new BigDecimal("0.0775"); // 7.75% for 3+ years
    }

    private TermDepositResponse mapToDto(TermDeposit td) {
        return new TermDepositResponse(
                td.getId(),
                td.getDepositNumber(),
                td.getCustomerId(),
                td.getLinkedAccountNumber(),
                td.getPrincipalAmount(),
                td.getCurrency(),
                td.getInterestRate(),
                td.getTenorMonths(),
                td.getCompoundingFrequency(),
                td.getMaturityAmount(),
                td.getMaturityDate(),
                td.getStatus(),
                td.isAutoRenewal(),
                td.getCreatedAt()
        );
    }
}
