package com.banking.bill.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "billers", schema = "bill_schema")
public class Biller {

    @Id
    private String id;

    @Column(name = "biller_code", nullable = false, unique = true, length = 32)
    private String billerCode;

    @Column(name = "biller_name", nullable = false, length = 100)
    private String billerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BillerCategory category;

    @Column(name = "service_fee", precision = 10, scale = 2)
    private BigDecimal serviceFee;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    public Biller() {}

    public Biller(String id, String billerCode, String billerName, BillerCategory category,
                  BigDecimal serviceFee, String currency) {
        this.id = id;
        this.billerCode = billerCode;
        this.billerName = billerName;
        this.category = category;
        this.serviceFee = serviceFee;
        this.currency = currency;
        this.active = true;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getBillerCode() { return billerCode; }
    public String getBillerName() { return billerName; }
    public BillerCategory getCategory() { return category; }
    public BigDecimal getServiceFee() { return serviceFee; }
    public String getCurrency() { return currency; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }

    public void setActive(boolean active) { this.active = active; }
}
