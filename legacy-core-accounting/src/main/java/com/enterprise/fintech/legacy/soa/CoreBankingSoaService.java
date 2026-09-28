package com.enterprise.fintech.legacy.soa;

import com.enterprise.fintech.legacy.LegacyAccountingEngine;
import com.enterprise.fintech.legacy.LegacyLedgerEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JAX-WS Web Service Endpoint simulating Oracle WebLogic SOA / OSB Core Banking Service.
 *
 * Exposes SOAP 1.1 / 1.2 XML interface for enterprise payment settlement.
 */
@WebService(
        name = "CoreBankingPostingService",
        targetNamespace = "http://soa.cbs.enterprise.fintech/v1"
)
public class CoreBankingSoaService {

    private static final Logger log = LoggerFactory.getLogger(CoreBankingSoaService.class);

    private final LegacyAccountingEngine accountingEngine;

    public CoreBankingSoaService() {
        this.accountingEngine = new LegacyAccountingEngine();
    }

    public CoreBankingSoaService(LegacyAccountingEngine accountingEngine) {
        this.accountingEngine = accountingEngine;
    }

    @WebMethod(operationName = "executePosting")
    @WebResult(name = "SoaPostingResponse")
    public SoaPostingResponse executePosting(@WebParam(name = "SoaPostingRequest") SoaPostingRequest request) {
        if (request == null || request.getCustomerAccountId() == null || request.getAmount() == null) {
            log.warn("[SOA Service] Received invalid posting request");
            return new SoaPostingResponse(
                    request != null ? request.getTransactionId() : "UNKNOWN",
                    null,
                    SoaPostingResponse.StatusCode.RJCT,
                    "Mandatory field validation failed"
            );
        }

        log.info("[SOA Service] Received SOAP/XML posting request: txnId={}, account={}, amount={} {}",
                request.getTransactionId(), request.getCustomerAccountId(), request.getAmount(), request.getCurrency());

        // Validate amount
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return new SoaPostingResponse(
                    request.getTransactionId(),
                    null,
                    SoaPostingResponse.StatusCode.RJCT,
                    "Amount must be strictly positive"
            );
        }

        String coreRef = "SOA-CBS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Debit Customer Account
        accountingEngine.recordEntry(new LegacyLedgerEntry(
                coreRef + "-DR",
                request.getCustomerAccountId(),
                request.getAmount(),
                LegacyLedgerEntry.EntryType.DEBIT,
                request.getCurrency(),
                LocalDate.now(),
                request.getRemarks()
        ));

        // Credit GL Account if supplied
        if (request.getGlAccountId() != null && !request.getGlAccountId().isEmpty()) {
            accountingEngine.recordEntry(new LegacyLedgerEntry(
                    coreRef + "-CR",
                    request.getGlAccountId(),
                    request.getAmount(),
                    LegacyLedgerEntry.EntryType.CREDIT,
                    request.getCurrency(),
                    LocalDate.now(),
                    "Contra: " + request.getRemarks()
            ));
        }

        log.info("[SOA Service] SOAP Posting Succeeded. Core Reference: {}", coreRef);

        return new SoaPostingResponse(
                request.getTransactionId(),
                coreRef,
                SoaPostingResponse.StatusCode.ACTC,
                "Settlement posted to Core Banking System via SOA Suite"
        );
    }

    @WebMethod(operationName = "pingService")
    @WebResult(name = "status")
    public String pingService() {
        return "CoreBankingSoaService is ACTIVE (Oracle WebLogic Baseline)";
    }
}
