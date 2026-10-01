-- Flyway Migration V1: Spring Batch High-Volume Staging Schema

CREATE TABLE stg_clearing_transactions (
    id VARCHAR(36) NOT NULL,
    external_txn_ref VARCHAR(64) NOT NULL,
    source_account VARCHAR(34) NOT NULL,
    target_account VARCHAR(34) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    settlement_date DATE NOT NULL,
    processing_status VARCHAR(20) NOT NULL DEFAULT 'STAGED',
    error_message VARCHAR(255),
    batch_job_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_stg_clearing PRIMARY KEY (id)
);

-- High-performance composite index for Oracle/PostgreSQL batch reader and PL/SQL MERGE
CREATE INDEX idx_stg_status_job ON stg_clearing_transactions (processing_status, batch_job_id);
CREATE INDEX idx_stg_settlement ON stg_clearing_transactions (settlement_date, processing_status);
