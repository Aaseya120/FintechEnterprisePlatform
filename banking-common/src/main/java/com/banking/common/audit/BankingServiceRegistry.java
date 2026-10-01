package com.banking.common.audit;

import lombok.Getter;

/**
 * Enterprise Service Registry:
 * Standardizes microservice identifiers (serviceId) across the banking ecosystem.
 * Each customer interaction across every service tags this serviceId in audit logs,
 * distributed tracing, and service catalogue permissions.
 */
@Getter
public enum BankingServiceRegistry {

    GATEWAY("SRV-GW-000", "API Gateway", "Gateway Routing, JWT Authentication & Redis Rate Limiting"),
    ONBOARDING_KYC("SRV-ONB-001", "Customer Onboarding & KYC Service", "Digital Customer Onboarding, Identity Proof & Video KYC"),
    ACCOUNT_LEDGER("SRV-ACC-002", "Account & Ledger Service", "Core Double-Entry Ledger, Balances, GraphQL & Legacy CBS Bridge"),
    PAYMENT_TRANSFER("SRV-PAY-003", "Payment & Transfer Service", "Multi-Rail Transfers (UPI, NEFT, IMPS, Cards, FX) & 2-Phase Saga"),
    EXCHANGE_RATE("SRV-FX-004", "Exchange Rate & Forex Service", "ISO Currencies, Real-Time Market Ticker & Guaranteed Quotes"),
    CARD_MANAGEMENT("SRV-CRD-005", "Card Management Service", "Debit & Credit Card Issuance, Luhn PAN, PIN Hashing & Security"),
    LOAN_LENDING("SRV-LON-006", "Loan & Lending Service", "Underwriting, Risk Assessment, Mathematical EMI & Amortization"),
    FRAUD_DETECTION("SRV-FRD-007", "Fraud Detection Service", "Sliding-Window Redis Velocity Rules & Real-Time Risk Engine"),
    NOTIFICATION("SRV-NOT-008", "Notification Service", "Omni-Channel Customer Alerts (SMS, Email, Push Notifications)"),
    REPORTING("SRV-REP-009", "Reporting & Statement Service", "Multi-Format Financial Statements (PDF, Excel, CSV, JSON) & Audits"),
    BATCH_CLEARING("SRV-BTC-010", "Batch & High-Volume Clearing Service", "Spring Batch Chunk Ingestion, Oracle 19c PL/SQL & Reconciliation");

    private final String serviceId;
    private final String serviceName;
    private final String description;

    BankingServiceRegistry(String serviceId, String serviceName, String description) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.description = description;
    }
}
