-- Flyway Migration V1: Financial Transfers and Transactional Outbox Schema
-- Designed for high throughput, idempotency, and Saga Orchestration

CREATE TABLE transfers (
    id VARCHAR(36) NOT NULL,
    saga_id VARCHAR(36) NOT NULL,
    idempotency_key VARCHAR(64) NOT NULL,
    source_account VARCHAR(34) NOT NULL,
    target_account VARCHAR(34) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(30) NOT NULL,
    failure_reason VARCHAR(255),
    channel VARCHAR(20) NOT NULL DEFAULT 'WEB',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_transfers PRIMARY KEY (id),
    CONSTRAINT uk_transfers_idempotency UNIQUE (idempotency_key)
);

-- Composite indexes for transfer queries
CREATE INDEX idx_transfers_saga ON transfers (saga_id, status);
CREATE INDEX idx_transfers_src_created ON transfers (source_account, created_at DESC);
CREATE INDEX idx_transfers_tgt_created ON transfers (target_account, created_at DESC);

-- Transactional Outbox Table for guaranteed at-least-once Kafka publishing
CREATE TABLE outbox_events (
    id VARCHAR(36) NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    topic VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_outbox PRIMARY KEY (id)
);

-- Index for high-throughput Outbox poller query
CREATE INDEX idx_outbox_status_created ON outbox_events (status, created_at);
