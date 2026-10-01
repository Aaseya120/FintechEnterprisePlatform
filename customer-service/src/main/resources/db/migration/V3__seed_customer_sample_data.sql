-- Flyway Migration V3: Seed Production-Grade Customer, KYC, and Beneficiary Sample Data

INSERT INTO customers (id, customer_number, first_name, last_name, email, phone, date_of_birth, address, risk_category, status, created_at, updated_at) VALUES
('CUST-001', 'CN-10001', 'Alexander', 'Hamilton', 'a.hamilton@banking-domain.com', '+12125550190', '1985-01-11', '55 Wall St, New York, NY 10005, USA', 'LOW', 'ACTIVE', NOW() - INTERVAL '120 days', NOW()),
('CUST-002', 'CN-10002', 'Eleanor', 'Vance', 'eleanor.vance@banking-domain.com', '+14155550182', '1990-07-22', '350 California St, San Francisco, CA 94104, USA', 'LOW', 'ACTIVE', NOW() - INTERVAL '90 days', NOW()),
('CUST-003', 'CN-10003', 'Julian', 'Schmidt', 'j.schmidt@banking-domain.de', '+49305550143', '1982-11-04', 'Friedrichstrasse 45, 10117 Berlin, Germany', 'LOW', 'ACTIVE', NOW() - INTERVAL '60 days', NOW()),
('CUST-004', 'CN-10004', 'Sophia', 'Windsor', 's.windsor@banking-domain.co.uk', '+442079460112', '1992-03-18', '25 Bank St, Canary Wharf, London E14 5JP, UK', 'LOW', 'ACTIVE', NOW() - INTERVAL '45 days', NOW()),
('CUST-005', 'CN-10005', 'Rajesh', 'Patel', 'rajesh.patel@banking-domain.in', '+919820055123', '1988-09-14', 'Bandra Kurla Complex, Mumbai 400051, India', 'MEDIUM', 'ACTIVE', NOW() - INTERVAL '30 days', NOW()),
('CUST-006', 'CN-10006', 'Tariq', 'Al-Mansoor', 'tariq.mansoor@banking-domain.ae', '+97143015555', '1984-06-30', 'DIFC Gate Precinct 4, Dubai, UAE', 'LOW', 'ACTIVE', NOW() - INTERVAL '15 days', NOW());

INSERT INTO customer_kyc (id, customer_id, id_type, id_number, document_url, verification_status, rejection_reason, verified_by, verified_at, created_at) VALUES
('KYC-001', 'CUST-001', 'PASSPORT', 'ENC:AES-GCM:US984729104', 'https://s3.amazonaws.com/banking-kyc-vault/docs/passports/cust-001.pdf', 'APPROVED', NULL, 'COMPLIANCE_OFFICER_01', NOW() - INTERVAL '119 days', NOW() - INTERVAL '120 days'),
('KYC-002', 'CUST-002', 'DRIVERS_LICENSE', 'ENC:AES-GCM:CA-DL-8291048', 'https://s3.amazonaws.com/banking-kyc-vault/docs/licenses/cust-002.pdf', 'APPROVED', NULL, 'COMPLIANCE_OFFICER_02', NOW() - INTERVAL '89 days', NOW() - INTERVAL '90 days'),
('KYC-003', 'CUST-003', 'NATIONAL_ID', 'ENC:AES-GCM:DE-ID-19827364', 'https://s3.amazonaws.com/banking-kyc-vault/docs/nat-id/cust-003.pdf', 'APPROVED', NULL, 'COMPLIANCE_OFFICER_EU', NOW() - INTERVAL '59 days', NOW() - INTERVAL '60 days'),
('KYC-004', 'CUST-005', 'PAN', 'ENC:AES-GCM:ABCDE1234F', 'https://s3.amazonaws.com/banking-kyc-vault/docs/pan/cust-005.pdf', 'APPROVED', NULL, 'COMPLIANCE_OFFICER_IN', NOW() - INTERVAL '29 days', NOW() - INTERVAL '30 days');

INSERT INTO beneficiaries (id, customer_id, beneficiary_name, account_number, bank_name, routing_or_ifsc_code, beneficiary_type, max_transfer_limit, cooling_end_time, is_active, created_at) VALUES
('BEN-001', 'CUST-001', 'Julian Schmidt', 'DE89370400440532013000', 'Deutsche Bank AG Berlin', 'DEUTDEDDFXX', 'INTERNATIONAL', 50000.0000, NOW() - INTERVAL '100 days', TRUE, NOW() - INTERVAL '100 days'),
('BEN-002', 'CUST-001', 'Sophia Windsor', 'GB29BARC20041538291044', 'Barclays Bank London', 'BARCGB22XXX', 'INTERNATIONAL', 75000.0000, NOW() - INTERVAL '80 days', TRUE, NOW() - INTERVAL '80 days'),
('BEN-003', 'CUST-001', 'Rajesh Patel', 'IN5000000001', 'State Bank of India', 'SBIN0001234', 'INTERNATIONAL', 25000.0000, NOW() - INTERVAL '20 days', TRUE, NOW() - INTERVAL '20 days'),
('BEN-004', 'CUST-001', 'Eleanor Vance', 'US2000000002', 'JPMorgan Chase NY', '021000021', 'DOMESTIC', 100000.0000, NOW() - INTERVAL '60 days', TRUE, NOW() - INTERVAL '60 days'),
('BEN-005', 'CUST-002', 'Alexander Hamilton', 'US1000000001', 'Bank of America NY', '026009593', 'DOMESTIC', 100000.0000, NOW() - INTERVAL '40 days', TRUE, NOW() - INTERVAL '40 days');
