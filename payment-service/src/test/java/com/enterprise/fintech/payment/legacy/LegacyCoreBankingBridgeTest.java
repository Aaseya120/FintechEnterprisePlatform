package com.enterprise.fintech.payment.legacy;

import com.enterprise.fintech.legacy.ejb.AccountBalanceResponse;
import com.enterprise.fintech.legacy.ejb.PostingResult;
import com.enterprise.fintech.legacy.soa.SoaPostingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class LegacyCoreBankingBridgeTest {

    private BdnsServiceResolver bdnsResolver;
    private LegacyCoreBankingSoaClient soaClient;
    private LegacyCoreBankingEjbBridge ejbBridge;

    @BeforeEach
    void setUp() {
        bdnsResolver = new BdnsServiceResolver();
        soaClient = new LegacyCoreBankingSoaClient(bdnsResolver);
        ejbBridge = new LegacyCoreBankingEjbBridge(bdnsResolver);
    }

    @Test
    void testSoaClientPostSettlement() {
        SoaPostingResponse response = soaClient.postSettlementToCoreBanking(
                "TXN-SOA-TEST-001",
                "CUST-ACC-8831",
                "GL-CLEARING-01",
                new BigDecimal("250.00"),
                "KWD",
                "E-Commerce settlement"
        );

        assertNotNull(response);
        assertEquals(SoaPostingResponse.StatusCode.ACTC, response.getStatusCode());
        assertNotNull(response.getCoreBankingReference());
        assertTrue(response.getCoreBankingReference().startsWith("SOA-CBS-"));
    }

    @Test
    void testEjbBridgePostLedgerEntry() {
        PostingResult result = ejbBridge.postLedgerEntry(
                "REF-EJB-TEST-002",
                "CUST-ACC-9942",
                "GL-NOSTRO-02",
                new BigDecimal("1000.00"),
                "KWD",
                "POS Card Transaction"
        );

        assertNotNull(result);
        assertEquals(PostingResult.Status.SUCCESS, result.getStatus());
        assertNotNull(result.getJournalEntryId());
        assertTrue(result.getJournalEntryId().startsWith("JRN-"));
    }

    @Test
    void testEjbBridgeQueryBalance() {
        AccountBalanceResponse balance = ejbBridge.getAccountBalance("CUST-ACC-9942");
        assertNotNull(balance);
        assertEquals("CUST-ACC-9942", balance.getAccountId());
    }

    @Test
    void testBdnsResolver() {
        String resolvedHost = bdnsResolver.resolveServiceHost("cbs-weblogic.fintech.enterprise.internal");
        assertNotNull(resolvedHost);
    }
}
