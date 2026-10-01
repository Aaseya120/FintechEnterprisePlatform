-- Flyway Migration V2: Seed Statement Export Audit Sample Data

INSERT INTO report_audit_logs (id, account_number, export_format, record_count, file_size_bytes, file_name, requested_by, exported_at) VALUES
('RPT-AUDIT-001', 'US1000000001', 'PDF', 14, 42890, 'statement_US1000000001.pdf', 'CUSTOMER_PORTAL', NOW() - INTERVAL '3 days'),
('RPT-AUDIT-002', 'US1000000001', 'EXCEL', 14, 18450, 'statement_US1000000001.xlsx', 'CUSTOMER_PORTAL', NOW() - INTERVAL '2 days'),
('RPT-AUDIT-003', 'DE89370400440532013000', 'CSV', 28, 5620, 'statement_DE89370400440532013000.csv', 'TELLER_BACKOFFICE', NOW() - INTERVAL '1 day'),
('RPT-AUDIT-004', 'GB29BARC20041538291044', 'JSON', 35, 12840, 'statement_GB29BARC20041538291044.json', 'API_INTEGRATION', NOW() - INTERVAL '6 hours');
