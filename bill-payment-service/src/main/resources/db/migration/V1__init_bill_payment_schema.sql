CREATE SCHEMA IF NOT EXISTS bill_schema;

CREATE TABLE IF NOT EXISTS bill_schema.billers (
    id VARCHAR(36) PRIMARY KEY,
    biller_code VARCHAR(32) NOT NULL UNIQUE,
    biller_name VARCHAR(100) NOT NULL,
    category VARCHAR(32) NOT NULL,
    service_fee NUMERIC(10, 2) DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bill_schema.bill_payments (
    id VARCHAR(36) PRIMARY KEY,
    payment_reference VARCHAR(64) NOT NULL UNIQUE,
    customer_id VARCHAR(36) NOT NULL,
    source_account_number VARCHAR(34) NOT NULL,
    biller_code VARCHAR(32) NOT NULL,
    consumer_number VARCHAR(64) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    service_fee NUMERIC(10, 2) DEFAULT 0.00,
    status VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(128),
    biller_transaction_ref VARCHAR(128),
    failure_reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_bill_payments_cust ON bill_schema.bill_payments(customer_id);
CREATE INDEX IF NOT EXISTS idx_bill_payments_ref ON bill_schema.bill_payments(payment_reference);
CREATE INDEX IF NOT EXISTS idx_billers_category ON bill_schema.billers(category);

-- Seed Initial Billers
INSERT INTO bill_schema.billers (id, biller_code, biller_name, category, service_fee, currency, active)
VALUES 
    ('biller-001', 'MEW-KUWAIT', 'Ministry of Electricity & Water (Kuwait)', 'UTILITY', 0.00, 'KWD', true),
    ('biller-002', 'ZAIN-KW', 'Zain Telecom Kuwait', 'TELECOM', 0.25, 'KWD', true),
    ('biller-003', 'OOREDOO-KW', 'Ooredoo Telecom Kuwait', 'TELECOM', 0.25, 'KWD', true),
    ('biller-004', 'K-NET-MUNICIPAL', 'Kuwait Municipality Civil Fees', 'MUNICIPALITY', 0.50, 'KWD', true)
ON CONFLICT (biller_code) DO NOTHING;
