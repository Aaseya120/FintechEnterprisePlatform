package com.enterprise.fintech.legacy.ejb;

import com.enterprise.fintech.legacy.LegacyAccountingEngine;
import com.enterprise.fintech.legacy.LegacyLedgerEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.UUID;

/**
 * Enterprise JavaBean (EJB 3.x) Stateless Session Bean.
 *
 * Implements core banking ledger settlement with container-managed
 * transactions (CMT) for deployment on Oracle WebLogic Application Server.
 */
@Stateless(name = "AccountPostingEJB")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class AccountPostingEJB implements AccountPostingRemote {

    private static final Logger log = LoggerFactory.getLogger(AccountPostingEJB.class);

    private final LegacyAccountingEngine accountingEngine;

    public AccountPostingEJB() {
        this.accountingEngine = new LegacyAccountingEngine();
    }

    public AccountPostingEJB(LegacyAccountingEngine accountingEngine) {
        this.accountingEngine = accountingEngine;
    }

    @Override
    public PostingResult postJournalEntry(JournalEntryRequest request) throws RemoteException {
        if (request == null || request.getAccountId() == null || request.getAmount() == null) {
            return new PostingResult(null, null, PostingResult.Status.REJECTED, "Invalid posting parameters");
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return new PostingResult(null, null, PostingResult.Status.REJECTED, "Amount must be strictly positive");
        }

        log.info("[WebLogic EJB] Processing Journal Entry: ref={}, account={}, amount={} {}",
                request.getTransactionReference(), request.getAccountId(), request.getAmount(), request.getCurrency());

        String journalId = "JRN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String coreRef = "CBS-" + System.currentTimeMillis();

        // 1. Debit Source Account
        LegacyLedgerEntry debitEntry = new LegacyLedgerEntry(
                journalId + "-DR",
                request.getAccountId(),
                request.getAmount(),
                LegacyLedgerEntry.EntryType.DEBIT,
                request.getCurrency(),
                request.getValueDate(),
                request.getNarration()
        );
        accountingEngine.recordEntry(debitEntry);

        // 2. Credit Contra / General Ledger Account
        if (request.getContraAccountId() != null && !request.getContraAccountId().isEmpty()) {
            LegacyLedgerEntry creditEntry = new LegacyLedgerEntry(
                    journalId + "-CR",
                    request.getContraAccountId(),
                    request.getAmount(),
                    LegacyLedgerEntry.EntryType.CREDIT,
                    request.getCurrency(),
                    request.getValueDate(),
                    "Contra: " + request.getNarration()
            );
            accountingEngine.recordEntry(creditEntry);
        }

        log.info("[WebLogic EJB] Successfully posted journal {} [CoreRef: {}]", journalId, coreRef);

        return new PostingResult(
                journalId,
                coreRef,
                PostingResult.Status.SUCCESS,
                "Journal posted successfully to Core Banking Ledger"
        );
    }

    @Override
    public AccountBalanceResponse queryBalance(String accountId) throws RemoteException {
        BigDecimal netBalance = accountingEngine.calculateNetBalance(accountId);
        log.info("[WebLogic EJB] Account balance query for {}: net={}", accountId, netBalance);
        String currency = accountingEngine.getAccountCurrency(accountId).orElse("KWD");
        return new AccountBalanceResponse(accountId, netBalance, netBalance, currency, "ACTIVE");
    }

    @Override
    public boolean isAccountActive(String accountId) throws RemoteException {
        return accountId != null && !accountId.startsWith("BLOCKED-");
    }

    public LegacyAccountingEngine getAccountingEngine() {
        return accountingEngine;
    }
}
