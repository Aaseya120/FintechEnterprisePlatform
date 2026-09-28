package com.enterprise.fintech.order.dto;

import com.enterprise.fintech.order.domain.Order;
import com.enterprise.fintech.order.domain.OrderStatus;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public class OrderResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderReference;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private OrderStatus status;
    private String invoiceDownloadUrl;
    private Instant createdAt;

    public OrderResponse() {}

    public static OrderResponse fromEntity(Order order) {
        OrderResponse resp = new OrderResponse();
        resp.setId(order.getId());
        resp.setOrderReference(order.getOrderReference());
        resp.setCustomerId(order.getCustomerId());
        resp.setAmount(order.getAmount());
        resp.setCurrency(order.getCurrency());
        resp.setStatus(order.getStatus());
        resp.setInvoiceDownloadUrl(order.getMinioInvoiceUrl());
        resp.setCreatedAt(order.getCreatedAt());
        return resp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderReference() { return orderReference; }
    public void setOrderReference(String orderReference) { this.orderReference = orderReference; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public String getInvoiceDownloadUrl() { return invoiceDownloadUrl; }
    public void setInvoiceDownloadUrl(String invoiceDownloadUrl) { this.invoiceDownloadUrl = invoiceDownloadUrl; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
