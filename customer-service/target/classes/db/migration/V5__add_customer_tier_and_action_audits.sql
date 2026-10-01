-- Flyway Migration V5: Customer Tiers (BASIC, PREMIUM, PLATINUM, HNI) and Cross-Service Action Audits

ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS customer_tier VARCHAR(20) DEFAULT 'BASIC' NOT NULL;

-- Customer Action Audits across all microservices (tracked via serviceId)
CREATE TABLE IF NOT EXISTS customer_action_audits (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    service_id VARCHAR(20) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(64),
    details VARCHAR(1000),
    channel VARCHAR(30),
    ip_address VARCHAR(45),
    status VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cust_audit_customer ON customer_action_audits (customer_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_cust_audit_service ON customer_action_audits (service_id, action_type);
