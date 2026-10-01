-- Flyway Migration V1: Initial Reporting & Statement Audit Schema

CREATE TABLE IF NOT EXISTS report_audit_logs (
    id VARCHAR(36) PRIMARY KEY,
    account_number VARCHAR(34) NOT NULL,
    export_format VARCHAR(10) NOT NULL,
    record_count INT NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    requested_by VARCHAR(50),
    exported_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_report_account_date ON report_audit_logs (account_number, exported_at);
