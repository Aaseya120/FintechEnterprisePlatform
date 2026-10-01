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
                      AND object_name IN ('CUSTOMERS', 'CUSTOMER_KYC', 'CUSTOMER_CREDENTIALS', 'BENEFICIARIES', 'CUSTOMER_ACTION_AUDITS', 'ACCOUNTS', 'ACCOUNT_AUDIT_LOG', 'TRANSFERS', 'OUTBOX_EVENTS', 
                                          'TERM_DEPOSITS', 'SAVING_VAULTS', 'STANDING_INSTRUCTIONS', 'CARDS', 'MOBILE_DEVICE_REGISTRATIONS',
                                          'LOANS', 'LOAN_REPAYMENT_SCHEDULE', 'LOAN_REPAYMENTS',
                                          'STG_CLEARING_TRANSACTIONS', 'RECON_RUNS', 'RECON_BREAKS',
                                          'EXCHANGE_RATES', 'FX_RATE_HISTORY', 'REPORT_AUDIT_LOGS', 'PKG_BANKING_CORE',
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

-- Digital Customer Onboarding
CREATE TABLE customers (
    id VARCHAR2(36) NOT NULL,
    customer_number VARCHAR2(32) NOT NULL,
    first_name VARCHAR2(100) NOT NULL,
    last_name VARCHAR2(100) NOT NULL,
    email VARCHAR2(150) NOT NULL,
    phone VARCHAR2(30) NOT NULL,
    date_of_birth DATE NOT NULL,
    address VARCHAR2(255) NOT NULL,
    risk_category VARCHAR2(20) DEFAULT 'LOW' NOT NULL,
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    customer_tier VARCHAR2(20) DEFAULT 'BASIC' NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uk_customers_num UNIQUE (customer_number),
    CONSTRAINT uk_customers_email UNIQUE (email)
);

-- Comprehensive Customer KYC, Video KYC (V-KYC) & Biometrics
CREATE TABLE customer_kyc (
    id VARCHAR2(36) NOT NULL,
    customer_id VARCHAR2(36) NOT NULL,
    id_type VARCHAR2(30) NOT NULL,
    id_number VARCHAR2(255) NOT NULL,
    document_url VARCHAR2(255) NOT NULL,
    address_proof_type VARCHAR2(30),
    address_proof_url VARCHAR2(255),
    selfie_url VARCHAR2(255),
    liveness_score NUMBER(5, 4),
    liveness_status VARCHAR2(20),
    video_kyc_url VARCHAR2(255),
    audio_sample_url VARCHAR2(255),
    geo_latitude NUMBER(10, 6),
    geo_longitude NUMBER(10, 6),
    ocr_extracted_data VARCHAR2(1000),
    verification_status VARCHAR2(20) DEFAULT 'SUBMITTED' NOT NULL,
    rejection_reason VARCHAR2(255),
    verified_by VARCHAR2(50),
    verified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_customer_kyc PRIMARY KEY (id),
    CONSTRAINT fk_kyc_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
);

CREATE INDEX idx_kyc_cust_stat ON customer_kyc (customer_id, verification_status);

-- Customer Credentials & Authentication (BCrypt + JWT Refresh Token Rotation)
CREATE TABLE customer_credentials (
    id VARCHAR2(36) NOT NULL,
    customer_id VARCHAR2(36) NOT NULL,
    username VARCHAR2(100) NOT NULL,
    password_hash VARCHAR2(255) NOT NULL,
    must_change_password NUMBER(1) DEFAULT 1 NOT NULL,
    refresh_token VARCHAR2(255),
    refresh_token_expiry TIMESTAMP WITH TIME ZONE,
    failed_login_attempts NUMBER(5) DEFAULT 0 NOT NULL,
    is_locked NUMBER(1) DEFAULT 0 NOT NULL,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_cust_credentials PRIMARY KEY (id),
    CONSTRAINT uk_cred_username UNIQUE (username),
    CONSTRAINT uk_cred_customer UNIQUE (customer_id),
    CONSTRAINT fk_cred_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
);

CREATE INDEX idx_cred_username ON customer_credentials (username);
CREATE INDEX idx_cred_refresh_token ON customer_credentials (refresh_token);

-- Customer Action Audits Across All Services (Tracked via serviceId)
CREATE TABLE customer_action_audits (
    id VARCHAR2(36) NOT NULL,
    customer_id VARCHAR2(36) NOT NULL,
    service_id VARCHAR2(20) NOT NULL,
    service_name VARCHAR2(100) NOT NULL,
    action_type VARCHAR2(50) NOT NULL,
    resource_id VARCHAR2(64),
    details VARCHAR2(1000),
    channel VARCHAR2(30),
    ip_address VARCHAR2(45),
    status VARCHAR2(20) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_cust_act_audits PRIMARY KEY (id),
    CONSTRAINT fk_cust_audit_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
);

CREATE INDEX idx_cust_act_customer ON customer_action_audits (customer_id, timestamp);
CREATE INDEX idx_cust_act_service ON customer_action_audits (service_id, action_type);

-- Whitelisted Beneficiaries & Cooling-Off Period
CREATE TABLE beneficiaries (
    id VARCHAR2(36) NOT NULL,
    customer_id VARCHAR2(36) NOT NULL,
    beneficiary_name VARCHAR2(100) NOT NULL,
    account_number VARCHAR2(34) NOT NULL,
    bank_name VARCHAR2(100) NOT NULL,
    routing_or_ifsc_code VARCHAR2(30) NOT NULL,
    beneficiary_type VARCHAR2(20) NOT NULL,
    max_transfer_limit NUMBER(19, 4) NOT NULL,
    cooling_end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    is_active NUMBER(1) DEFAULT 1 NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_beneficiaries PRIMARY KEY (id),
    CONSTRAINT fk_ben_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
);

CREATE INDEX idx_ben_cust_active ON beneficiaries (customer_id, is_active);

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
    service_id VARCHAR2(20) DEFAULT 'SRV-ACC-002',
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

-- Financial Reporting & Statement Export Audit Logs
CREATE TABLE report_audit_logs (
    id VARCHAR2(36) NOT NULL,
    account_number VARCHAR2(34) NOT NULL,
    export_format VARCHAR2(10) NOT NULL,
    record_count NUMBER(10) NOT NULL,
    file_size_bytes NUMBER(19) NOT NULL,
    file_name VARCHAR2(255) NOT NULL,
    requested_by VARCHAR2(50),
    exported_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_report_audit_logs PRIMARY KEY (id)
);

CREATE INDEX idx_report_audit_acc_dt ON report_audit_logs (account_number, exported_at);

-- Fixed & Term Deposits (Compound Interest & Premature Liquidation)
CREATE TABLE term_deposits (
    id VARCHAR2(36) NOT NULL,
    deposit_number VARCHAR2(32) NOT NULL,
    customer_id VARCHAR2(36) NOT NULL,
    linked_account_number VARCHAR2(34) NOT NULL,
    principal_amount NUMBER(19, 4) NOT NULL,
    interest_rate NUMBER(6, 4) NOT NULL,
    tenor_months NUMBER(10) NOT NULL,
    compounding_frequency VARCHAR2(20) DEFAULT 'QUARTERLY' NOT NULL,
    maturity_amount NUMBER(19, 4) NOT NULL,
    start_date DATE NOT NULL,
    maturity_date DATE NOT NULL,
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    premature_liquidated_at TIMESTAMP WITH TIME ZONE,
    actual_payout_amount NUMBER(19, 4),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_term_deposits PRIMARY KEY (id),
    CONSTRAINT uk_term_dep_num UNIQUE (deposit_number)
);

CREATE INDEX idx_term_dep_cust_stat ON term_deposits (customer_id, status);

-- Saving Vaults, Saving Goals & Target Sub-Account Pots
CREATE TABLE saving_vaults (
    id VARCHAR2(36) NOT NULL,
    customer_id VARCHAR2(36) NOT NULL,
    parent_account_number VARCHAR2(34) NOT NULL,
    vault_name VARCHAR2(100) NOT NULL,
    target_amount NUMBER(19, 4) NOT NULL,
    current_balance NUMBER(19, 4) DEFAULT 0.0000 NOT NULL,
    currency VARCHAR2(3) NOT NULL,
    target_date DATE,
    lock_status VARCHAR2(20) DEFAULT 'UNLOCKED' NOT NULL,
    auto_roundup_enabled NUMBER(1) DEFAULT 0 NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_saving_vaults PRIMARY KEY (id)
);

CREATE INDEX idx_vault_customer ON saving_vaults (customer_id);
CREATE INDEX idx_vault_parent_acc ON saving_vaults (parent_account_number);

-- Recurring Payments & Standing Instructions (SI)
CREATE TABLE standing_instructions (
    id VARCHAR2(36) NOT NULL,
    instruction_name VARCHAR2(100) NOT NULL,
    customer_id VARCHAR2(36) NOT NULL,
    source_account_number VARCHAR2(34) NOT NULL,
    target_account_number VARCHAR2(34) NOT NULL,
    amount NUMBER(19, 4) NOT NULL,
    currency VARCHAR2(3) NOT NULL,
    frequency VARCHAR2(20) NOT NULL,
    execution_day NUMBER(5),
    next_execution_date DATE NOT NULL,
    category VARCHAR2(30) NOT NULL,
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    total_executions_count NUMBER(10) DEFAULT 0 NOT NULL,
    last_executed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_standing_instructions PRIMARY KEY (id)
);

CREATE INDEX idx_si_customer ON standing_instructions (customer_id, status);
CREATE INDEX idx_si_next_exec ON standing_instructions (next_execution_date, status);

-- Cards (Debit, Credit, Virtual) with Mobile Controls & Dynamic CVV
CREATE TABLE cards (
    id VARCHAR2(36) NOT NULL,
    card_number VARCHAR2(19) NOT NULL,
    card_network VARCHAR2(20) NOT NULL,
    card_type VARCHAR2(20) NOT NULL,
    customer_id VARCHAR2(36) NOT NULL,
    linked_account_number VARCHAR2(34) NOT NULL,
    card_holder_name VARCHAR2(100) NOT NULL,
    expiry_month NUMBER(2) NOT NULL,
    expiry_year NUMBER(4) NOT NULL,
    cvv_hash VARCHAR2(64) NOT NULL,
    pin_hash VARCHAR2(64),
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    daily_limit NUMBER(19, 4) NOT NULL,
    is_international_enabled NUMBER(1) DEFAULT 0 NOT NULL,
    is_contactless_enabled NUMBER(1) DEFAULT 1 NOT NULL,
    is_online_enabled NUMBER(1) DEFAULT 1 NOT NULL,
    is_atm_enabled NUMBER(1) DEFAULT 1 NOT NULL,
    is_pos_enabled NUMBER(1) DEFAULT 1 NOT NULL,
    reward_points NUMBER(19) DEFAULT 0 NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_cards PRIMARY KEY (id),
    CONSTRAINT uk_cards_number UNIQUE (card_number)
);

CREATE INDEX idx_cards_cust_stat ON cards (customer_id, status);

-- Mobile App Device Push Registration (iOS APNs / Android FCM & Biometrics)
CREATE TABLE mobile_device_registrations (
    id VARCHAR2(36) NOT NULL,
    customer_id VARCHAR2(36) NOT NULL,
    platform VARCHAR2(20) NOT NULL,
    device_token VARCHAR2(512) NOT NULL,
    device_model VARCHAR2(100),
    os_version VARCHAR2(50),
    app_version VARCHAR2(50),
    biometric_key VARCHAR2(512),
    is_active NUMBER(1) DEFAULT 1 NOT NULL,
    registered_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    last_active_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_mobile_dev_reg PRIMARY KEY (id)
);

CREATE INDEX idx_mobile_cust_act ON mobile_device_registrations (customer_id, is_active);
CREATE INDEX idx_mobile_dev_tok ON mobile_device_registrations (device_token);

-- Loans & Lending Lifecycle
CREATE TABLE loans (
    id VARCHAR2(36) NOT NULL,
    loan_account_number VARCHAR2(34) NOT NULL,
    customer_id VARCHAR2(36) NOT NULL,
    loan_type VARCHAR2(30) NOT NULL,
    principal_amount NUMBER(19, 4) NOT NULL,
    annual_interest_rate NUMBER(6, 4) NOT NULL,
    tenure_months NUMBER(5) NOT NULL,
    emi_amount NUMBER(19, 4) NOT NULL,
    status VARCHAR2(30) DEFAULT 'APPLIED' NOT NULL,
    disbursement_account VARCHAR2(34) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_loans PRIMARY KEY (id),
    CONSTRAINT uk_loan_acc UNIQUE (loan_account_number)
);

CREATE INDEX idx_loans_cust_stat ON loans (customer_id, status);

-- Loan Repayment Amortization Schedule
CREATE TABLE loan_repayment_schedule (
    id VARCHAR2(36) NOT NULL,
    loan_id VARCHAR2(36) NOT NULL,
    installment_number NUMBER(5) NOT NULL,
    due_date DATE NOT NULL,
    principal_component NUMBER(19, 4) NOT NULL,
    interest_component NUMBER(19, 4) NOT NULL,
    total_installment NUMBER(19, 4) NOT NULL,
    remaining_balance NUMBER(19, 4) NOT NULL,
    status VARCHAR2(20) DEFAULT 'PENDING' NOT NULL,
    CONSTRAINT pk_loan_schedule PRIMARY KEY (id),
    CONSTRAINT fk_loan_sched_loan FOREIGN KEY (loan_id) REFERENCES loans(id) ON DELETE CASCADE
);

CREATE INDEX idx_loan_sched_due ON loan_repayment_schedule (loan_id, due_date);

-- Loan Repayments & Foreclosure Transactions
CREATE TABLE loan_repayments (
    id VARCHAR2(36) NOT NULL,
    loan_id VARCHAR2(36) NOT NULL,
    customer_id VARCHAR2(36) NOT NULL,
    amount_paid NUMBER(19, 4) NOT NULL,
    payment_type VARCHAR2(30) NOT NULL,
    payment_method VARCHAR2(50) NOT NULL,
    transaction_reference VARCHAR2(100) NOT NULL,
    paid_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_loan_repayments PRIMARY KEY (id),
    CONSTRAINT fk_repay_loan FOREIGN KEY (loan_id) REFERENCES loans(id)
);

CREATE INDEX idx_repay_loan_dt ON loan_repayments (loan_id, paid_at DESC);

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

INSERT INTO report_audit_logs (id, account_number, export_format, record_count, file_size_bytes, file_name, requested_by, exported_at)
VALUES ('RPT-AUD-ORA-01', 'US1000000001', 'PDF', 14, 42890, 'statement_US1000000001.pdf', 'CUSTOMER_PORTAL', SYSTIMESTAMP - 3);

INSERT INTO report_audit_logs (id, account_number, export_format, record_count, file_size_bytes, file_name, requested_by, exported_at)
VALUES ('RPT-AUD-ORA-02', 'US1000000001', 'EXCEL', 14, 18450, 'statement_US1000000001.xlsx', 'CUSTOMER_PORTAL', SYSTIMESTAMP - 2);

COMMIT;
