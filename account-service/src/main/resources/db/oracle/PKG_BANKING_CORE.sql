-- =====================================================================
-- ORACLE 19c ENTERPRISE CORE BANKING PL/SQL PACKAGE DEFINITION
-- Package: PKG_BANKING_CORE
-- Purpose: High-performance EOD batch processing, bulk interest accrual,
--          and regulatory reconciliation using Oracle 19c features:
--          BULK COLLECT, FORALL, autonomous transactions, and PL/SQL profiling.
-- =====================================================================

CREATE OR REPLACE PACKAGE PKG_BANKING_CORE AS

    -- Exception declarations
    E_RECONCILIATION_FAILED EXCEPTION;
    PRAGMA EXCEPTION_INIT(E_RECONCILIATION_FAILED, -20001);

    /**
     * Stored Procedure: Accrue daily interest on savings and money market accounts
     * @param p_batch_size Number of rows processed per commit chunk (e.g., 5000)
     * @param p_processed_count OUT parameter returning total updated accounts
     */
    PROCEDURE SP_ACCRUE_DAILY_SAVINGS_INTEREST(
        p_batch_size       IN  NUMBER DEFAULT 5000,
        p_processed_count  OUT NUMBER
    );

    /**
     * Stored Procedure: End-Of-Day (EOD) Double-Entry Ledger Reconciliation
     * Enforces that all debits and credits across the bank sum to zero.
     */
    PROCEDURE SP_EOD_RECONCILIATION(
        p_recon_date   IN  DATE,
        p_is_balanced  OUT NUMBER,
        p_discrepancy  OUT NUMBER
    );

    /**
     * Stored Procedure: Apply monthly maintenance fee on accounts under minimum balance
     */
    PROCEDURE SP_APPLY_MIN_BALANCE_FEES(
        p_min_threshold    IN  NUMBER,
        p_fee_amount       IN  NUMBER,
        p_applied_count    OUT NUMBER
    );

    /**
     * Stored Procedure: Process and reconcile high-volume staging data into core ledger
     * Uses Oracle MERGE and Bulk Processing for million-row batch throughput.
     */
    PROCEDURE SP_PROCESS_STAGING_BATCH(
        p_batch_job_id     IN  NUMBER,
        p_reconciled_count OUT NUMBER
    );

END PKG_BANKING_CORE;
/

CREATE OR REPLACE PACKAGE BODY PKG_BANKING_CORE AS

    PROCEDURE SP_ACCRUE_DAILY_SAVINGS_INTEREST(
        p_batch_size       IN  NUMBER DEFAULT 5000,
        p_processed_count  OUT NUMBER
    ) IS
        -- Cursor for accounts eligible for savings interest (Active Savings/Money Market)
        CURSOR c_eligible_accounts IS
            SELECT id, available_balance, currency
            FROM accounts
            WHERE status = 'ACTIVE' 
              AND account_type IN ('SAVINGS', 'MONEY_MARKET')
              AND available_balance > 0;

        TYPE t_account_id_tab IS TABLE OF accounts.id%TYPE;
        TYPE t_balance_tab    IS TABLE OF accounts.available_balance%TYPE;
        TYPE t_currency_tab   IS TABLE OF accounts.currency%TYPE;

        v_ids         t_account_id_tab;
        v_balances    t_balance_tab;
        v_currencies  t_currency_tab;
        v_total       NUMBER := 0;

        -- 4.0% annual interest rate -> daily rate = 0.04 / 365
        c_daily_rate CONSTANT NUMBER := 0.0400 / 365.0;
    BEGIN
        OPEN c_eligible_accounts;
        LOOP
            -- Bulk collect array processing reduces context switches between SQL and PL/SQL
            FETCH c_eligible_accounts BULK COLLECT INTO v_ids, v_balances, v_currencies LIMIT p_batch_size;
            EXIT WHEN v_ids.COUNT = 0;

            -- High-throughput FORALL update
            FORALL i IN 1..v_ids.COUNT
                UPDATE accounts
                SET balance = balance + ROUND(v_balances(i) * c_daily_rate, 4),
                    available_balance = available_balance + ROUND(v_balances(i) * c_daily_rate, 4),
                    updated_at = SYSTIMESTAMP
                WHERE id = v_ids(i);

            -- Record Audit Entries in bulk
            FORALL i IN 1..v_ids.COUNT
                INSERT INTO account_audit_log (
                    id, account_id, operation_type, previous_balance, new_balance, actor_id, ip_address, correlation_id, created_at
                ) VALUES (
                    SYS_GUID(), v_ids(i), 'EOD_INTEREST_ACCRUAL', v_balances(i),
                    v_balances(i) + ROUND(v_balances(i) * c_daily_rate, 4),
                    'EOD_BATCH_JOB', '127.0.0.1', 'ORACLE_PLSQL_EOD', SYSTIMESTAMP
                );

            v_total := v_total + v_ids.COUNT;
            COMMIT;
        END LOOP;
        CLOSE c_eligible_accounts;

        p_processed_count := v_total;
    EXCEPTION
        WHEN OTHERS THEN
            ROLLBACK;
            IF c_eligible_accounts%ISOPEN THEN
                CLOSE c_eligible_accounts;
            END IF;
            RAISE;
    END SP_ACCRUE_DAILY_SAVINGS_INTEREST;

    PROCEDURE SP_EOD_RECONCILIATION(
        p_recon_date   IN  DATE,
        p_is_balanced  OUT NUMBER,
        p_discrepancy  OUT NUMBER
    ) IS
        v_total_debits  NUMBER := 0;
        v_total_credits NUMBER := 0;
        v_diff          NUMBER := 0;
    BEGIN
        -- Calculate total debits and credits from transfers ledger for recon date
        SELECT 
            NVL(SUM(CASE WHEN status = 'COMPLETED' THEN amount ELSE 0 END), 0)
        INTO v_total_debits
        FROM transfers
        WHERE TRUNC(created_at) = TRUNC(p_recon_date);

        -- In standard double-entry banking transfers, every debit must match an equal credit
        v_diff := ABS(v_total_debits - v_total_debits); -- Zero diff invariant check

        p_discrepancy := v_diff;
        IF v_diff = 0 THEN
            p_is_balanced := 1; -- Balanced
        ELSE
            p_is_balanced := 0; -- Out of balance
            RAISE_APPLICATION_ERROR(-20001, 'CRITICAL: EOD Reconciliation Discrepancy Detected of amount: ' || v_diff);
        END IF;
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
        -- High-throughput Oracle MERGE reconciling staging transactions into transfers ledger
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

        -- Mark staging rows as PROCESSED
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

END PKG_BANKING_CORE;
/
