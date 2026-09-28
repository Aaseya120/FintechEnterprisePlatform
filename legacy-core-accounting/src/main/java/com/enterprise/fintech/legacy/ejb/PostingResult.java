package com.enterprise.fintech.legacy.ejb;

import java.io.Serializable;
import java.time.Instant;

/**
 * Result DTO returned by Core Banking EJB posting operations.
 */
public class PostingResult implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Status {
        SUCCESS,
        REJECTED,
        INSUFFICIENT_FUNDS,
        ACCOUNT_BLOCKED
    }

    private String journalEntryId;
    private String coreReference;
    private Status status;
    private String responseMessage;
    private Instant postedAt;

    public PostingResult() {}

    public PostingResult(String journalEntryId, String coreReference, Status status, String responseMessage) {
        this.journalEntryId = journalEntryId;
        this.coreReference = coreReference;
        this.status = status;
        this.responseMessage = responseMessage;
        this.postedAt = Instant.now();
    }

    public String getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(String journalEntryId) { this.journalEntryId = journalEntryId; }

    public String getCoreReference() { return coreReference; }
    public void setCoreReference(String coreReference) { this.coreReference = coreReference; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getResponseMessage() { return responseMessage; }
    public void setResponseMessage(String responseMessage) { this.responseMessage = responseMessage; }

    public Instant getPostedAt() { return postedAt; }
    public void setPostedAt(Instant postedAt) { this.postedAt = postedAt; }
}
