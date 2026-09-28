package com.enterprise.fintech.order.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "FIN_OUTBOX_EVENTS", indexes = {
        @Index(name = "idx_outbox_status", columnList = "processed")
})
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 64)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "processed", nullable = false)
    private boolean processed = false;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public OutboxEvent() {}

    public OutboxEvent(String aggregateType, String aggregateId, String eventType, String payload, String traceId) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.traceId = traceId;
        this.processed = false;
        this.retryCount = 0;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getAggregateType() { return aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public boolean isProcessed() { return processed; }
    public void setProcessed(boolean processed) { this.processed = processed; }
    public int getRetryCount() { return retryCount; }
    public void incrementRetry() { this.retryCount++; }
    public String getTraceId() { return traceId; }
    public Instant getCreatedAt() { return createdAt; }
}
