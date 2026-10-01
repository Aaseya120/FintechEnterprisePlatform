-- Flyway Migration V3: Add standard service_id to account_audit_log

ALTER TABLE account_audit_log
    ADD COLUMN IF NOT EXISTS service_id VARCHAR(20) DEFAULT 'SRV-ACC-002';
