-- Flyway Migration V2: Seed Production-Grade Account Sample Data

INSERT INTO accounts (id, account_number, customer_id, account_type, currency, balance, available_balance, status, version, created_at, updated_at) VALUES
('ACC-001', 'US1000000001', 'CUST-001', 'CHECKING', 'USD', 25450.7500, 25450.7500, 'ACTIVE', 0, NOW() - INTERVAL '90 days', NOW()),
('ACC-002', 'US1000000002', 'CUST-001', 'SAVINGS', 'USD', 142800.0000, 142800.0000, 'ACTIVE', 0, NOW() - INTERVAL '90 days', NOW()),
('ACC-003', 'US2000000002', 'CUST-002', 'CHECKING', 'USD', 8920.5000, 8920.5000, 'ACTIVE', 0, NOW() - INTERVAL '60 days', NOW()),
('ACC-004', 'EU3000000001', 'CUST-003', 'CHECKING', 'EUR', 45100.2500, 45100.2500, 'ACTIVE', 0, NOW() - INTERVAL '45 days', NOW()),
('ACC-005', 'GB4000000001', 'CUST-004', 'CHECKING', 'GBP', 32750.8000, 32750.8000, 'ACTIVE', 0, NOW() - INTERVAL '30 days', NOW()),
('ACC-006', 'IN5000000001', 'CUST-005', 'SAVINGS', 'INR', 850000.0000, 850000.0000, 'ACTIVE', 0, NOW() - INTERVAL '120 days', NOW()),
('ACC-007', 'AE6000000001', 'CUST-006', 'CHECKING', 'AED', 95400.0000, 95400.0000, 'ACTIVE', 0, NOW() - INTERVAL '15 days', NOW());

INSERT INTO account_audit_log (id, account_id, operation_type, previous_balance, new_balance, actor_id, ip_address, correlation_id, created_at) VALUES
('AUD-001', 'ACC-001', 'ACCOUNT_OPENING_DEPOSIT', 0.0000, 25450.7500, 'TELLER_104', '192.168.1.10', 'CORR-INIT-001', NOW() - INTERVAL '90 days'),
('AUD-002', 'ACC-002', 'ACCOUNT_OPENING_DEPOSIT', 0.0000, 142800.0000, 'TELLER_104', '192.168.1.10', 'CORR-INIT-002', NOW() - INTERVAL '90 days'),
('AUD-003', 'ACC-003', 'ACCOUNT_OPENING_DEPOSIT', 0.0000, 8920.5000, 'TELLER_108', '192.168.1.12', 'CORR-INIT-003', NOW() - INTERVAL '60 days'),
('AUD-004', 'ACC-001', 'PAYROLL_DIRECT_DEPOSIT', 21450.7500, 25450.7500, 'ACH_FED_CLEARING', '10.0.4.15', 'CORR-ACH-4491', NOW() - INTERVAL '2 days');
