-- Flyway Migration V2: Seed Production-Grade Fraud Alerts and Risk Evaluation Sample Data

INSERT INTO fraud_alerts (id, transaction_id, account_number, amount, risk_score, risk_decision, triggered_rules, client_ip, location, created_at) VALUES
('FRD-001', 'TXN-1001', 'US1000000001', 250.0000, 15, 'APPROVED', 'LOW_VELOCITY, DOMESTIC_DEVICE', '192.168.1.100', 'New York, US', NOW() - INTERVAL '5 days'),
('FRD-002', 'TXN-1002', 'US1000000001', 4500.0000, 45, 'APPROVED', 'CROSS_BORDER_SEPA_BENEFICIARY_VERIFIED', '192.168.1.100', 'New York, US', NOW() - INTERVAL '4 days'),
('FRD-003', 'TXN-1007', 'US1000000001', 15000.0000, 92, 'BLOCKED', 'LARGE_AMOUNT_THRESHOLD_EXCEEDED, UNRECOGNIZED_VPN_IP', '185.220.101.5', 'Moscow, RU', NOW() - INTERVAL '1 hour');
