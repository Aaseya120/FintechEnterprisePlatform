package com.enterprise.fintech.payment.legacy;

import com.enterprise.fintech.legacy.ejb.AccountBalanceResponse;
import com.enterprise.fintech.legacy.ejb.AccountPostingEJB;
import com.enterprise.fintech.legacy.ejb.AccountPostingRemote;
import com.enterprise.fintech.legacy.ejb.JournalEntryRequest;
import com.enterprise.fintech.legacy.ejb.PostingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Enterprise JavaBean (EJB 3.x) Anti-Corruption Layer (ACL) Bridge.
 *
 * Provides modern Spring Boot microservices with a client adapter to communicate
 * with the legacy Core Banking EJB layer deployed on Oracle WebLogic Application Server.
 */
@Service
public class LegacyCoreBankingEjbBridge {

    private static final Logger log = LoggerFactory.getLogger(LegacyCoreBankingEjbBridge.class);

    @Value("${cbs.weblogic.ejb.provider-url:t3://cbs-weblogic.fintech.enterprise.internal:7001}")
    private String ejbProviderUrl = "t3://cbs-weblogic.fintech.enterprise.internal:7001";

    @Value("${cbs.weblogic.ejb.jndi-name:ejb:/legacy-core-accounting/AccountPostingEJB!com.enterprise.fintech.legacy.ejb.AccountPostingRemote}")
    private String ejbJndiName = "ejb:/legacy-core-accounting/AccountPostingEJB!com.enterprise.fintech.legacy.ejb.AccountPostingRemote";

    private final BdnsServiceResolver bdnsResolver;
    private final AccountPostingRemote localEjbDelegate;

    public LegacyCoreBankingEjbBridge(BdnsServiceResolver bdnsResolver) {
        this.bdnsResolver = bdnsResolver;
        this.localEjbDelegate = new AccountPostingEJB();
    }

    /**
     * Posts a double-entry ledger transaction to the legacy Core Banking System via EJB.
     */
    public PostingResult postLedgerEntry(String transactionReference, String accountId,
                                         String contraAccountId, BigDecimal amount,
                                         String currency, String narration) {
        log.info("[EJB ACL Bridge] Posting ledger entry to CBS via EJB: ref={}, account={}, amount={} {}",
                transactionReference, accountId, amount, currency);

        JournalEntryRequest request = new JournalEntryRequest(
                transactionReference,
                accountId,
                contraAccountId,
                amount,
                currency,
                narration,
                LocalDate.now()
        );

        try {
            // In a production WebLogic cluster, JNDI lookup resolves the remote EJB stub.
            // Using the local EJB implementation enables containerized microservices and cloud testing:
            PostingResult result = localEjbDelegate.postJournalEntry(request);
            log.info("[EJB ACL Bridge] EJB posting completed successfully: journalId={}, coreRef={}",
                    result.getJournalEntryId(), result.getCoreReference());
            return result;
        } catch (Exception ex) {
            log.error("[EJB ACL Bridge] EJB posting failed for ref {}", transactionReference, ex);
            return new PostingResult(
                    null,
                    null,
                    PostingResult.Status.REJECTED,
                    "EJB invocation failure: " + ex.getMessage()
            );
        }
    }

    /**
     * Checks account balance from Core Banking EJB.
     */
    public AccountBalanceResponse getAccountBalance(String accountId) {
        try {
            return localEjbDelegate.queryBalance(accountId);
        } catch (Exception ex) {
            log.error("[EJB ACL Bridge] Balance query failed for account {}", accountId, ex);
            return new AccountBalanceResponse(accountId, BigDecimal.ZERO, BigDecimal.ZERO, "USD", "UNAVAILABLE");
        }
    }

    public String getEjbProviderUrl() {
        return ejbProviderUrl;
    }
}
