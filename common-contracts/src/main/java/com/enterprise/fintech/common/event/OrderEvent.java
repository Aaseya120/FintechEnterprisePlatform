package com.enterprise.fintech.common.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public class OrderEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum EventType {
        ORDER_CREATED,
        ORDER_PAYMENT_PENDING,
        ORDER_CONFIRMED,
        ORDER_CANCELLED,
        ORDER_COMPENSATING
    }

    private String eventId;
    private String orderId;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private EventType eventType;
    private Instant timestamp;
    private String traceId;

    public OrderEvent() {}

    public OrderEvent(String eventId, String orderId, String customerId, BigDecimal amount,
                      String currency, EventType eventType, Instant timestamp, String traceId) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.traceId = traceId;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    @Override
    public String toString() {
        return "OrderEvent{" +
                "eventId='" + eventId + '\'' +
                ", orderId='" + orderId + '\'' +
                ", eventType=" + eventType +
                ", amount=" + amount +
                ", traceId='" + traceId + '\'' +
                '}';
    }
}
