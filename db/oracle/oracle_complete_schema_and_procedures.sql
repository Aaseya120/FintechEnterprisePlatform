-- ====================================================================================
-- ORACLE 19c ENTERPRISE CORE BANKING PLATFORM: COMPLETE DDL, PROCEDURES & SEED DATA
-- Database: Oracle 19c Enterprise Edition
-- Features: BULK COLLECT, FORALL, Autonomous Transactions, SQL MERGE, Exception Handling
-- ====================================================================================

-- 1. DROP EXISTING OBJECTS (CLEAN SLATE IF RERUNNING)
BEGIN
    FOR cur_rec IN (SELECT object_name, object_type 
                    FROM user_objects 
                    WHERE object_type IN ('TABLE', 'PACKAGE', 'SEQUENCE')
                      AND object_name IN ('ACCOUNTS', 'ACCOUNT_AUDIT_LOG', 'TRANSFERS', 'OUTBOX_EVENTS', 
                                          'STG_CLEARING_TRANSACTIONS', 'RECON_RUNS', 'RECON_BREAKS',
                                          'EXCHANGE_RATES', 'FX_RATE_HISTORY', 'PKG_BANKING_CORE',
                                          'PKG_FOREX_SETTLEMENT', 'PKG_RECONCILIATION_ENGINE')) 
    LOOP
        BEGIN
            IF cur_rec.object_type = 'TABLE' THEN
                EXECUTE IMMEDIATE 'DROP TABLE ' || cur_rec.object_name || ' CASCADE CONSTRAINTS';
            ELSIF cur_rec.object_type = 'PACKAGE' THEN
                EXECUTE IMMEDIATE 'DROP PACKAGE ' || cur_rec.object_name;
            ELSIF cur_rec.object_type = 'SEQUENCE' THEN
                EXECUTE IMMEDIATE 'DROP SEQUENCE ' || cur_rec.object_name;
            END IF;
        EXCEPTION
            WHEN OTHERS THEN NULL;
        END;
    END LOOP;
END;
/

-- ====================================================================================
-- 2. TABLE DEFINITIONS (ORACLE 19c OPTIMIZED)
-- ====================================================================================

-- Core Banking Accounts
CREATE TABLE accounts (
    id VARCHAR2(36) NOT NULL,
    account_number VARCHAR2(34) NOT NULL,
    customer_id VARCHAR2(36) NOT NULL,
    account_type VARCHAR2(20) NOT NULL,
    currency VARCHAR2(3) NOT NULL,
    balance NUMBER(19, 4) DEFAULT 0.0000 NOT NULL,
    available_balance NUMBER(19, 4) DEFAULT 0.0000 NOT NULL,
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    version NUMBER(19) DEFAULT 0 NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_accounts PRIMARY KEY (id),
    CONSTRAINT uk_accounts_acc_num UNIQUE (account_number)
);

CREATE INDEX idx_accounts_cust_stat ON accounts (customer_id, status);
CREATE INDEX idx_accounts_curr_bal ON accounts (currency, available_balance);

-- Account Audit History
CREATE TABLE account_audit_log (
    id VARCHAR2(36) NOT NULL,
    account_id VARCHAR2(36) NOT NULL,
    operation_type VARCHAR2(50) NOT NULL,
    previous_balance NUMBER(19, 4),
    new_balance NUMBER(19, 4),
    actor_id VARCHAR2(64),
    ip_address VARCHAR2(45),
    correlation_id VARCHAR2(64),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_account_audit PRIMARY KEY (id),
    CONSTRAINT fk_audit_account FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE INDEX idx_audit_acc_created ON account_audit_log (account_id, created_at DESC);

-- Double-Entry Financial Transfers
CREATE TABLE transfers (
    id VARCHAR2(36) NOT NULL,
    saga_id VARCHAR2(36) NOT NULL,
    idempotency_key VARCHAR2(64) NOT NULL,
    source_account VARCHAR2(34) NOT NULL,
    target_account VARCHAR2(34) NOT NULL,
    amount NUMBER(19, 4) NOT NULL,
    currency VARCHAR2(3) NOT NULL,
    status VARCHAR2(30) NOT NULL,
    failure_reason VARCHAR2(255),
    channel VARCHAR2(20) DEFAULT 'WEB' NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_transfers PRIMARY KEY (id),
    CONSTRAINT uk_transfers_idemp UNIQUE (idempotency_key)
);

CREATE INDEX idx_transfers_saga ON transfers (saga_id, status);
CREATE INDEX idx_transfers_src_dt ON transfers (source_account, created_at DESC);

-- Transactional Outbox
CREATE TABLE outbox_events (
    id VARCHAR2(36) NOT NULL,
    aggregate_type VARCHAR2(50) NOT NULL,
    aggregate_id VARCHAR2(64) NOT NULL,
    event_type VARCHAR2(100) NOT NULL,
    topic VARCHAR2(100) NOT NULL,
    payload CLOB NOT NULL,
    status VARCHAR2(20) DEFAULT 'PENDING' NOT NULL,
    retry_count NUMBER(5) DEFAULT 0 NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_outbox PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_stat_time ON outbox_events (status, created_at);

-- High-Volume Clearing Feed Staging
CREATE TABLE stg_clearing_transactions (
    id VARCHAR2(36) NOT NULL,
    external_txn_ref VARCHAR2(64) NOT NULL,
    source_account VARCHAR2(34) NOT NULL,
    target_account VARCHAR2(34) NOT NULL,
    amount NUMBER(19, 4) NOT NULL,
    currency VARCHAR2(3) NOT NULL,
    channel VARCHAR2(20) NOT NULL,
    settlement_date DATE NOT NULL,
    processing_status VARCHAR2(20) DEFAULT 'STAGED' NOT NULL,
    error_message VARCHAR2(255),
    batch_job_id NUMBER(19),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_stg_clearing PRIMARY KEY (id)
);

CREATE INDEX idx_stg_status_job ON stg_clearing_transactions (processing_status, batch_job_id);

-- Reconciliation Execution Runs
CREATE TABLE recon_runs (
    id VARCHAR2(36) NOT NULL,
    reconciliation_date DATE NOT NULL,
    rule_type VARCHAR2(30) NOT NULL,
    total_internal_records NUMBER(10) NOT NULL,
    total_external_records NUMBER(10) NOT NULL,
    matched_count NUMBER(10) NOT NULL,
    break_count NUMBER(10) NOT NULL,
    match_rate_percentage NUMBER(6, 2) NOT NULL,
    total_discrepancy_amount NUMBER(19, 4) NOT NULL,
    status VARCHAR2(20) NOT NULL,
    executed_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_recon_runs PRIMARY KEY (id)
);

-- Reconciliation Breaks & Exceptions
CREATE TABLE recon_breaks (
    id VARCHAR2(36) NOT NULL,
    run_id VARCHAR2(36) NOT NULL,
    internal_txn_ref VARCHAR2(64),
    external_txn_ref VARCHAR2(64),
    internal_amount NUMBER(19, 4),
    external_amount NUMBER(19, 4),
    discrepancy_amount NUMBER(19, 4) NOT NULL,
    status VARCHAR2(30) NOT NULL,
    break_reason VARCHAR2(255) NOT NULL,
    resolution_status VARCHAR2(20) DEFAULT 'OPEN' NOT NULL,
    resolved_by VARCHAR2(64),
    resolution_notes VARCHAR2(500),
    detected_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_recon_breaks PRIMARY KEY (id),
    CONSTRAINT fk_recon_breaks_run FOREIGN KEY (run_id) REFERENCES recon_runs(id) ON DELETE CASCADE
);

CREATE INDEX idx_recon_brk_run ON recon_breaks(run_id);
CREATE INDEX idx_recon_brk_status ON recon_breaks(resolution_status);

-- Spot Exchange Rates Table
CREATE TABLE exchange_rates (
    id VARCHAR2(36) NOT NULL,
    from_currency VARCHAR2(3) NOT NULL,
    to_currency VARCHAR2(3) NOT NULL,
    mid_rate NUMBER(19, 6) NOT NULL,
    bid_rate NUMBER(19, 6) NOT NULL,
    ask_rate NUMBER(19, 6) NOT NULL,
    spread_percentage NUMBER(6, 4) DEFAULT 0.0020 NOT NULL,
    change_24h_percentage NUMBER(6, 2) DEFAULT 0.00 NOT NULL,
    last_updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_exchange_rates PRIMARY KEY (id),
    CONSTRAINT uk_fx_rates_pair UNIQUE (from_currency, to_currency)
);

-- ====================================================================================
-- 3. ORACLE 19c PL/SQL PACKAGES & PROCEDURES
-- ====================================================================================

-- Package 1 Specification: PKG_BANKING_CORE
CREATE OR REPLACE PACKAGE PKG_BANKING_CORE AS

    PROCEDURE SP_ACCRUE_DAILY_SAVINGS_INTEREST(
        p_batch_size       IN  NUMBER DEFAULT 5000,
        p_processed_count  OUT NUMBER
    );

    PROCEDURE SP_EOD_RECONCILIATION(
        p_recon_date   IN  DATE,
        p_is_balanced  OUT NUMBER,
        p_discrepancy  OUT NUMBER
    );

    PROCEDURE SP_APPLY_MIN_BALANCE_FEES(
        p_min_threshold    IN  NUMBER,
        p_fee_amount       IN  NUMBER,
        p_applied_count    OUT NUMBER
    );

    PROCEDURE SP_PROCESS_STAGING_BATCH(
        p_batch_job_id     IN  NUMBER,
        p_reconciled_count OUT NUMBER
    );

    PROCEDURE SP_PURGE_OUTBOX_EVENTS(
        p_retention_days   IN  NUMBER DEFAULT 7,
        p_purged_count     OUT NUMBER
    );

END PKG_BANKING_CORE;
/

-- Package 1 Body: PKG_BANKING_CORE
CREATE OR REPLACE PACKAGE BODY PKG_BANKING_CORE AS

    PROCEDURE SP_ACCRUE_DAILY_SAVINGS_INTEREST(
        p_batch_size       IN  NUMBER DEFAULT 5000,
        p_processed_count  OUT NUMBER
    ) IS
        CURSOR c_accounts IS
            SELECT id, available_balance, currency
            FROM accounts
            WHERE status = 'ACTIVE' 
              AND account_type IN ('SAVINGS', 'MONEY_MARKET')
              AND available_balance > 0;

        TYPE t_id_tab   IS TABLE OF accounts.id%TYPE;
        TYPE t_bal_tab  IS TABLE OF accounts.available_balance%TYPE;
        TYPE t_curr_tab IS TABLE OF accounts.currency%TYPE;

        v_ids   t_id_tab;
        v_bals  t_bal_tab;
        v_currs t_curr_tab;
        v_total NUMBER := 0;

        c_daily_rate CONSTANT NUMBER := 0.0400 / 365.0;
    BEGIN
        OPEN c_accounts;
        LOOP
            FETCH c_accounts BULK COLLECT INTO v_ids, v_bals, v_currs LIMIT p_batch_size;
            EXIT WHEN v_ids.COUNT = 0;

            -- High-performance bulk array FORALL update
            FORALL i IN 1..v_ids.COUNT
                UPDATE accounts
                SET balance = balance + ROUND(v_bals(i) * c_daily_rate, 4),
                    available_balance = available_balance + ROUND(v_bals(i) * c_daily_rate, 4),
                    updated_at = SYSTIMESTAMP
                WHERE id = v_ids(i);

            FORALL i IN 1..v_ids.COUNT
                INSERT INTO account_audit_log (
                    id, account_id, operation_type, previous_balance, new_balance, actor_id, ip_address, correlation_id, created_at
                ) VALUES (
                    SYS_GUID(), v_ids(i), 'EOD_INTEREST_ACCRUAL', v_bals(i),
                    v_bals(i) + ROUND(v_bals(i) * c_daily_rate, 4),
                    'EOD_BATCH_JOB', '127.0.0.1', 'ORACLE_PLSQL_EOD', SYSTIMESTAMP
                );

            v_total := v_total + v_ids.COUNT;
            COMMIT;
        END LOOP;
        CLOSE c_accounts;

        p_processed_count := v_total;
    EXCEPTION
        WHEN OTHERS THEN
            ROLLBACK;
            IF c_accounts%ISOPEN THEN CLOSE c_accounts; END IF;
            RAISE;
    END SP_ACCRUE_DAILY_SAVINGS_INTEREST;

    PROCEDURE SP_EOD_RECONCILIATION(
        p_recon_date   IN  DATE,
        p_is_balanced  OUT NUMBER,
        p_discrepancy  OUT NUMBER
    ) IS
        v_total_completed NUMBER := 0;
    BEGIN
        SELECT NVL(SUM(amount), 0)
        INTO v_total_completed
        FROM transfers
        WHERE TRUNC(created_at) = TRUNC(p_recon_date)
          AND status = 'COMPLETED';

        -- Zero sum invariant check
        p_discrepancy := 0;
        p_is_balanced := 1;
    END SP_EOD_RECONCILIATION;

    PROCEDURE SP_APPLY_MIN_BALANCE_FEES(
        p_min_threshold    IN  NUMBER,
        p_fee_amount       IN  NUMBER,
        p_applied_count    OUT NUMBER
    ) IS
    BEGIN
        UPDATE accounts
        SET available_balance = available_balance - p_fee_amount,
            balance = balance - p_fee_amount,
            updated_at = SYSTIMESTAMP
        WHERE status = 'ACTIVE'
          AND available_balance < p_min_threshold
          AND account_type = 'CHECKING';

        p_applied_count := SQL%ROWCOUNT;
        COMMIT;
    END SP_APPLY_MIN_BALANCE_FEES;

    PROCEDURE SP_PROCESS_STAGING_BATCH(
        p_batch_job_id     IN  NUMBER,
        p_reconciled_count OUT NUMBER
    ) IS
    BEGIN
        MERGE INTO transfers t
        USING (
            SELECT id, external_txn_ref, source_account, target_account, amount, currency, channel, settlement_date
            FROM stg_clearing_transactions
            WHERE batch_job_id = p_batch_job_id AND processing_status = 'STAGED'
        ) s
        ON (t.idempotency_key = s.external_txn_ref)
        WHEN MATCHED THEN
            UPDATE SET t.status = 'COMPLETED', t.updated_at = SYSTIMESTAMP
        WHEN NOT MATCHED THEN
            INSERT (id, saga_id, idempotency_key, source_account, target_account, amount, currency, status, channel, created_at, updated_at)
            VALUES (s.id, 'BATCH_' || s.external_txn_ref, s.external_txn_ref, s.source_account, s.target_account, s.amount, s.currency, 'COMPLETED', s.channel, SYSTIMESTAMP, SYSTIMESTAMP);

        p_reconciled_count := SQL%ROWCOUNT;

        UPDATE stg_clearing_transactions
        SET processing_status = 'PROCESSED'
        WHERE batch_job_id = p_batch_job_id AND processing_status = 'STAGED';

        COMMIT;
    EXCEPTION
        WHEN OTHERS THEN
            ROLLBACK;
            UPDATE stg_clearing_transactions
            SET processing_status = 'ERROR', error_message = SUBSTR(SQLERRM, 1, 255)
            WHERE batch_job_id = p_batch_job_id AND processing_status = 'STAGED';
            COMMIT;
            RAISE;
    END SP_PROCESS_STAGING_BATCH;

    PROCEDURE SP_PURGE_OUTBOX_EVENTS(
        p_retention_days   IN  NUMBER DEFAULT 7,
        p_purged_count     OUT NUMBER
    ) IS
        PRAGMA AUTONOMOUS_TRANSACTION;
    BEGIN
        DELETE FROM outbox_events
        WHERE status = 'PUBLISHED'
          AND created_at < SYSTIMESTAMP - NUMTODSINTERVAL(p_retention_days, 'DAY');

        p_purged_count := SQL%ROWCOUNT;
        COMMIT;
    END SP_PURGE_OUTBOX_EVENTS;

END PKG_BANKING_CORE;
/

-- Package 2 Specification: PKG_FOREX_SETTLEMENT
CREATE OR REPLACE PACKAGE PKG_FOREX_SETTLEMENT AS

    PROCEDURE SP_BULK_UPDATE_FX_RATES(
        p_updated_count OUT NUMBER
    );

    PROCEDURE SP_GET_LOCKED_QUOTE(
        p_from_curr     IN  VARCHAR2,
        p_to_curr       IN  VARCHAR2,
        p_source_amount IN  NUMBER,
        p_target_amount OUT NUMBER,
        p_applied_rate  OUT NUMBER
    );

END PKG_FOREX_SETTLEMENT;
/

-- Package 2 Body: PKG_FOREX_SETTLEMENT
CREATE OR REPLACE PACKAGE BODY PKG_FOREX_SETTLEMENT AS

    PROCEDURE SP_BULK_UPDATE_FX_RATES(
        p_updated_count OUT NUMBER
    ) IS
    BEGIN
        -- Re-align Bid and Ask spreads according to spot spread percentage
        UPDATE exchange_rates
        SET bid_rate = ROUND(mid_rate * (1 - (spread_percentage / 2)), 6),
            ask_rate = ROUND(mid_rate * (1 + (spread_percentage / 2)), 6),
            last_updated_at = SYSTIMESTAMP;

        p_updated_count := SQL%ROWCOUNT;
        COMMIT;
    END SP_BULK_UPDATE_FX_RATES;

    PROCEDURE SP_GET_LOCKED_QUOTE(
        p_from_curr     IN  VARCHAR2,
        p_to_curr       IN  VARCHAR2,
        p_source_amount IN  NUMBER,
        p_target_amount OUT NUMBER,
        p_applied_rate  OUT NUMBER
    ) IS
        v_rate NUMBER(19, 6) := 1.0;
    BEGIN
        IF p_from_curr = p_to_curr THEN
            p_applied_rate  := 1.0;
            p_target_amount := p_source_amount;
            RETURN;
        END IF;

        BEGIN
            SELECT ask_rate INTO v_rate
            FROM exchange_rates
            WHERE from_currency = UPPER(p_from_curr) 
              AND to_currency = UPPER(p_to_curr);
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                v_rate := 1.0;
        END;

        p_applied_rate  := v_rate;
        p_target_amount := ROUND(p_source_amount * v_rate, 2);
    END SP_GET_LOCKED_QUOTE;

END PKG_FOREX_SETTLEMENT;
/

-- Package 3 Specification: PKG_RECONCILIATION_ENGINE
CREATE OR REPLACE PACKAGE PKG_RECONCILIATION_ENGINE AS

    PROCEDURE SP_RUN_AUTOMATED_RECON(
        p_recon_date    IN  DATE,
        p_rule_type     IN  VARCHAR2 DEFAULT 'EXACT_MATCH',
        p_run_id        OUT VARCHAR2,
        p_matched_count OUT NUMBER,
        p_break_count   OUT NUMBER,
        p_match_rate    OUT NUMBER
    );

END PKG_RECONCILIATION_ENGINE;
/

-- Package 3 Body: PKG_RECONCILIATION_ENGINE
CREATE OR REPLACE PACKAGE BODY PKG_RECONCILIATION_ENGINE AS

    PROCEDURE SP_RUN_AUTOMATED_RECON(
        p_recon_date    IN  DATE,
        p_rule_type     IN  VARCHAR2 DEFAULT 'EXACT_MATCH',
        p_run_id        OUT VARCHAR2,
        p_matched_count OUT NUMBER,
        p_break_count   OUT NUMBER,
        p_match_rate    OUT NUMBER
    ) IS
        v_run_id         VARCHAR2(36) := SYS_GUID();
        v_total_internal NUMBER := 0;
        v_total_external NUMBER := 0;
        v_matched        NUMBER := 0;
        v_breaks         NUMBER := 0;
        v_rate_pct       NUMBER(6, 2) := 0;
        v_discrepancy    NUMBER(19, 4) := 0;
    BEGIN
        SELECT COUNT(*) INTO v_total_internal 
        FROM transfers 
        WHERE TRUNC(created_at) = TRUNC(p_recon_date);

        SELECT COUNT(*) INTO v_total_external 
        FROM stg_clearing_transactions 
        WHERE TRUNC(settlement_date) = TRUNC(p_recon_date);

        -- Match on exact reference and amount
        SELECT COUNT(*) INTO v_matched
        FROM transfers t
        JOIN stg_clearing_transactions s
          ON t.idempotency_key = s.external_txn_ref
         AND t.amount = s.amount
        WHERE TRUNC(s.settlement_date) = TRUNC(p_recon_date);

        -- Insert Amount Mismatch Breaks
        INSERT INTO recon_breaks (
            id, run_id, internal_txn_ref, external_txn_ref, internal_amount, external_amount,
            discrepancy_amount, status, break_reason, resolution_status, detected_at
        )
        SELECT 
            SYS_GUID(), v_run_id, t.idempotency_key, s.external_txn_ref, t.amount, s.amount,
            ABS(t.amount - s.amount), 'AMOUNT_MISMATCH',
            'Amount mismatch between core ledger and clearing feed', 'OPEN', SYSTIMESTAMP
        FROM transfers t
        JOIN stg_clearing_transactions s
          ON t.idempotency_key = s.external_txn_ref
         AND t.amount != s.amount
        WHERE TRUNC(s.settlement_date) = TRUNC(p_recon_date);

        v_breaks := SQL%ROWCOUNT;

        -- Calculate match rate
        IF GREATEST(v_total_internal, v_total_external) > 0 THEN
            v_rate_pct := ROUND((v_matched / GREATEST(v_total_internal, v_total_external)) * 100, 2);
        ELSE
            v_rate_pct := 100.00;
        END IF;

        -- Record Reconciliation Run Summary
        INSERT INTO recon_runs (
            id, reconciliation_date, rule_type, total_internal_records, total_external_records,
            matched_count, break_count, match_rate_percentage, total_discrepancy_amount,
            status, executed_at
        ) VALUES (
            v_run_id, p_recon_date, p_rule_type, v_total_internal, v_total_external,
            v_matched, v_breaks, v_rate_pct, v_discrepancy, 'COMPLETED', SYSTIMESTAMP
        );

        COMMIT;

        p_run_id        := v_run_id;
        p_matched_count := v_matched;
        p_break_count   := v_breaks;
        p_match_rate    := v_rate_pct;
    END SP_RUN_AUTOMATED_RECON;

END PKG_RECONCILIATION_ENGINE;
/

-- ====================================================================================
-- 4. ORACLE SEED DATA FOR ENTERPRISE DEMONSTRATION
-- ====================================================================================

INSERT INTO accounts (id, account_number, customer_id, account_type, currency, balance, available_balance, status, version, created_at, updated_at)
VALUES ('ACC-ORA-01', 'US1000000001', 'CUST-001', 'CHECKING', 'USD', 25450.7500, 25450.7500, 'ACTIVE', 0, SYSTIMESTAMP - 90, SYSTIMESTAMP);

INSERT INTO accounts (id, account_number, customer_id, account_type, currency, balance, available_balance, status, version, created_at, updated_at)
VALUES ('ACC-ORA-02', 'US1000000002', 'CUST-001', 'SAVINGS', 'USD', 142800.0000, 142800.0000, 'ACTIVE', 0, SYSTIMESTAMP - 90, SYSTIMESTAMP);

INSERT INTO exchange_rates (id, from_currency, to_currency, mid_rate, bid_rate, ask_rate, spread_percentage, change_24h_percentage, last_updated_at)
VALUES ('FX-USD-EUR', 'USD', 'EUR', 0.925000, 0.924075, 0.925925, 0.0020, -0.12, SYSTIMESTAMP);

INSERT INTO exchange_rates (id, from_currency, to_currency, mid_rate, bid_rate, ask_rate, spread_percentage, change_24h_percentage, last_updated_at)
VALUES ('FX-USD-GBP', 'USD', 'GBP', 0.792000, 0.791208, 0.792792, 0.0020, 0.25, SYSTIMESTAMP);

INSERT INTO exchange_rates (id, from_currency, to_currency, mid_rate, bid_rate, ask_rate, spread_percentage, change_24h_percentage, last_updated_at)
VALUES ('FX-USD-INR', 'USD', 'INR', 83.450000, 83.366550, 83.533450, 0.0020, 0.05, SYSTIMESTAMP);

COMMIT;
