-- Flyway Migration V3: Seed Production-Grade Clearing and Reconciliation Sample Data

INSERT INTO stg_clearing_transactions (id, external_txn_ref, source_account, target_account, amount, currency, channel, settlement_date, processing_status, batch_job_id, created_at) VALUES
('STG-001', 'FEDWIRE-2026-9001', 'US1000000001', 'US2000000002', 250.0000, 'USD', 'FEDWIRE', CURRENT_DATE, 'STAGED', 101, NOW()),
('STG-002', 'TARGET2-2026-9002', 'US1000000001', 'DE89370400440532013000', 4500.0000, 'EUR', 'TARGET2', CURRENT_DATE, 'STAGED', 101, NOW()),
('STG-003', 'CHAPS-2026-9003', 'US1000000002', 'GB29BARC20041538291044', 1200.0000, 'GBP', 'CHAPS', CURRENT_DATE, 'STAGED', 101, NOW()),
('STG-004', 'SWIFT-2026-9004', 'US1000000001', 'IN5000000001', 501.2500, 'USD', 'SWIFT', CURRENT_DATE, 'STAGED', 101, NOW()), -- Variance test
('STG-005', 'ACH-2026-9005', 'US2000000002', 'US1000000001', 75.5000, 'USD', 'ACH', CURRENT_DATE, 'STAGED', 101, NOW());

INSERT INTO recon_runs (id, reconciliation_date, rule_type, total_internal_records, total_external_records, matched_count, break_count, match_rate_percentage, total_discrepancy_amount, status, executed_at) VALUES
('RUN-2026-1001', CURRENT_DATE - INTERVAL '1 day', 'EXACT_MATCH', 500, 502, 498, 4, 99.20, 1501.2500, 'COMPLETED', NOW() - INTERVAL '1 day');

INSERT INTO recon_breaks (id, run_id, internal_txn_ref, external_txn_ref, internal_amount, external_amount, discrepancy_amount, status, break_reason, resolution_status, resolved_by, resolution_notes, detected_at, resolved_at) VALUES
('BRK-001', 'RUN-2026-1001', 'TXN-1004', 'SWIFT-2026-9004', 500.0000, 501.2500, 1.2500, 'AMOUNT_MISMATCH', 'Intermediary SWIFT routing surcharge difference', 'RESOLVED', 'AUDITOR_402', 'Adjusted to clearing fee expense ledger account', NOW() - INTERVAL '1 day', NOW() - INTERVAL '18 hours'),
('BRK-002', 'RUN-2026-1001', 'TXN-ORPHAN-4491', NULL, 1500.0000, NULL, 1500.0000, 'UNMATCHED_INTERNAL', 'Core ledger transfer missing from external clearing batch', 'OPEN', NULL, NULL, NOW() - INTERVAL '1 day', NULL);
