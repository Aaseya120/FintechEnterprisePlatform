-- Flyway Migration V2: Reconciliation Runs and Breaks Schema

CREATE TABLE IF NOT EXISTS recon_runs (
    id VARCHAR(36) NOT NULL,
    reconciliation_date DATE NOT NULL,
    rule_type VARCHAR(30) NOT NULL,
    total_internal_records INT NOT NULL,
    total_external_records INT NOT NULL,
    matched_count INT NOT NULL,
    break_count INT NOT NULL,
    match_rate_percentage NUMERIC(6, 2) NOT NULL,
    total_discrepancy_amount NUMERIC(19, 4) NOT NULL,
    status VARCHAR(20) NOT NULL,
    executed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_recon_runs PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS recon_breaks (
    id VARCHAR(36) NOT NULL,
    run_id VARCHAR(36) NOT NULL,
    internal_txn_ref VARCHAR(64),
    external_txn_ref VARCHAR(64),
    internal_amount NUMERIC(19, 4),
    external_amount NUMERIC(19, 4),
    discrepancy_amount NUMERIC(19, 4) NOT NULL,
    status VARCHAR(30) NOT NULL,
    break_reason VARCHAR(255) NOT NULL,
    resolution_status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    resolved_by VARCHAR(64),
    resolution_notes VARCHAR(500),
    detected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_recon_breaks PRIMARY KEY (id),
    CONSTRAINT fk_recon_breaks_run FOREIGN KEY (run_id) REFERENCES recon_runs(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_recon_breaks_run ON recon_breaks(run_id);
CREATE INDEX IF NOT EXISTS idx_recon_breaks_res_status ON recon_breaks(resolution_status);
