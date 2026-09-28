package com.enterprise.fintech.legacy;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class LegacyAccountingEngineTest {

    private LegacyAccountingEngine engine;

    @Before
    public void setUp() {
        engine = new LegacyAccountingEngine();
        engine.recordEntry(new LegacyLedgerEntry("E1", "ACC-100", new BigDecimal("1000.00"),
                LegacyLedgerEntry.EntryType.CREDIT, "USD", LocalDate.of(2026, 1, 15), "Initial deposit"));
        engine.recordEntry(new LegacyLedgerEntry("E2", "ACC-100", new BigDecimal("250.00"),
                LegacyLedgerEntry.EntryType.DEBIT, "USD", LocalDate.of(2026, 1, 20), "Card payment"));
        engine.recordEntry(new LegacyLedgerEntry("E3", "ACC-200", new BigDecimal("500.00"),
                LegacyLedgerEntry.EntryType.CREDIT, "USD", LocalDate.of(2026, 2, 1), "Salary"));
    }

    @Test
    public void testCalculateNetBalance() {
        BigDecimal balance = engine.calculateNetBalance("ACC-100");
        assertEquals(new BigDecimal("750.00"), balance);
    }

    @Test
    public void testGroupingByAccount() {
        Map<String, List<LegacyLedgerEntry>> grouped = engine.groupEntriesByAccount();
        assertEquals(2, grouped.size());
        assertEquals(2, grouped.get("ACC-100").size());
        assertEquals(1, grouped.get("ACC-200").size());
    }

    @Test
    public void testFindEntriesInDateRange() {
        List<LegacyLedgerEntry> janEntries = engine.findEntriesInDateRange(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        );
        assertEquals(2, janEntries.size());
    }

    @Test
    public void testCalculateTotalCreditsByCurrency() {
        BigDecimal totalUSD = engine.calculateTotalCreditsByCurrency("USD");
        assertEquals(new BigDecimal("1500.00"), totalUSD);
    }

    @Test
    public void testAccountPostingEJB() throws Exception {
        com.enterprise.fintech.legacy.ejb.AccountPostingEJB ejb =
                new com.enterprise.fintech.legacy.ejb.AccountPostingEJB(engine);

        com.enterprise.fintech.legacy.ejb.JournalEntryRequest request =
                new com.enterprise.fintech.legacy.ejb.JournalEntryRequest(
                        "TXN-EJB-001",
                        "ACC-100",
                        "GL-NOSTRO-01",
                        new BigDecimal("150.00"),
                        "USD",
                        "ATM Cash Withdrawal",
                        LocalDate.now()
                );

        com.enterprise.fintech.legacy.ejb.PostingResult result = ejb.postJournalEntry(request);
        assertEquals(com.enterprise.fintech.legacy.ejb.PostingResult.Status.SUCCESS, result.getStatus());

        com.enterprise.fintech.legacy.ejb.AccountBalanceResponse balance = ejb.queryBalance("ACC-100");
        assertEquals(new BigDecimal("600.00"), balance.getCurrentBalance());
        assertEquals("USD", balance.getCurrency());
    }

    @Test
    public void testGetAccountCurrency() {
        assertEquals("USD", engine.getAccountCurrency("ACC-100").orElse(""));
        assertEquals(java.util.Optional.empty(), engine.getAccountCurrency("NON-EXISTENT"));
    }

    @Test
    public void testCoreBankingSoaService() {
        com.enterprise.fintech.legacy.soa.CoreBankingSoaService soaService =
                new com.enterprise.fintech.legacy.soa.CoreBankingSoaService(engine);

        com.enterprise.fintech.legacy.soa.SoaPostingRequest request =
                new com.enterprise.fintech.legacy.soa.SoaPostingRequest(
                        "SOA-REQ-001",
                        "ACC-200",
                        "GL-FEE-INCOME",
                        new BigDecimal("50.00"),
                        "USD",
                        "MOBILE_BANKING",
                        "Cross-border transfer fee"
                );

        com.enterprise.fintech.legacy.soa.SoaPostingResponse response = soaService.executePosting(request);
        assertEquals(com.enterprise.fintech.legacy.soa.SoaPostingResponse.StatusCode.ACTC, response.getStatusCode());
        assertEquals("CoreBankingSoaService is ACTIVE (Oracle WebLogic Baseline)", soaService.pingService());
    }
}
