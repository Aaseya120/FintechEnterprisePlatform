package com.enterprise.fintech.legacy.soa;

import java.io.Serializable;
import java.time.Instant;

/**
 * JAXB / SOAP XML Response payload for Core Banking SOA Web Service.
 */
public class SoaPostingResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum StatusCode {
        ACTC, // Accepted Technical Validation
        RJCT, // Rejected
        ACSP  // Accepted Settlement In Process
    }

    private String transactionId;
    private String coreBankingReference;
    private StatusCode statusCode;
    private String statusDescription;
    private Instant processedTimestamp;

    public SoaPostingResponse() {}

    public SoaPostingResponse(String transactionId, String coreBankingReference,
                              StatusCode statusCode, String statusDescription) {
        this.transactionId = transactionId;
        this.coreBankingReference = coreBankingReference;
        this.statusCode = statusCode;
        this.statusDescription = statusDescription;
        this.processedTimestamp = Instant.now();
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getCoreBankingReference() { return coreBankingReference; }
    public void setCoreBankingReference(String coreBankingReference) { this.coreBankingReference = coreBankingReference; }

    public StatusCode getStatusCode() { return statusCode; }
    public void setStatusCode(StatusCode statusCode) { this.statusCode = statusCode; }

    public String getStatusDescription() { return statusDescription; }
    public void setStatusDescription(String statusDescription) { this.statusDescription = statusDescription; }

    public Instant getProcessedTimestamp() { return processedTimestamp; }
    public void setProcessedTimestamp(Instant processedTimestamp) { this.processedTimestamp = processedTimestamp; }
}
