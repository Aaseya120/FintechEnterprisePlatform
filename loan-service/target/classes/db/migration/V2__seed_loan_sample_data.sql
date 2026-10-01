-- Flyway Migration V2: Seed Production-Grade Loan and Repayment Amortization Sample Data

INSERT INTO loans (id, loan_account_number, customer_id, loan_type, principal_amount, annual_interest_rate, tenure_months, emi_amount, status, disbursement_account, created_at, updated_at) VALUES
('LN-001', 'LN1000000001', 'CUST-001', 'PERSONAL_LOAN', 25000.0000, 0.0850, 24, 1136.6300, 'DISBURSED', 'US1000000001', NOW() - INTERVAL '60 days', NOW() - INTERVAL '60 days'),
('LN-002', 'LN1000000002', 'CUST-002', 'HOME_MORTGAGE', 350000.0000, 0.0625, 240, 2558.1200, 'APPROVED', 'US2000000002', NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days');

INSERT INTO loan_repayment_schedule (id, loan_id, installment_number, due_date, principal_component, interest_component, total_installment, remaining_balance, status) VALUES
('SCH-001', 'LN-001', 1, CURRENT_DATE - INTERVAL '30 days', 959.5500, 177.0800, 1136.6300, 24040.4500, 'PAID'),
('SCH-002', 'LN-001', 2, CURRENT_DATE, 966.3400, 170.2900, 1136.6300, 23074.1100, 'PAID'),
('SCH-003', 'LN-001', 3, CURRENT_DATE + INTERVAL '30 days', 973.1900, 163.4400, 1136.6300, 22100.9200, 'PENDING'),
('SCH-004', 'LN-001', 4, CURRENT_DATE + INTERVAL '60 days', 980.0800, 156.5500, 1136.6300, 21120.8400, 'PENDING'),
('SCH-005', 'LN-001', 5, CURRENT_DATE + INTERVAL '90 days', 987.0300, 149.6000, 1136.6300, 20133.8100, 'PENDING'),
('SCH-006', 'LN-001', 6, CURRENT_DATE + INTERVAL '120 days', 994.0200, 142.6100, 1136.6300, 19139.7900, 'PENDING');
