-- Flyway Migration V2: Seed Production-Grade Payment and Outbox Sample Data

INSERT INTO transfers (id, saga_id, idempotency_key, source_account, target_account, amount, currency, status, failure_reason, channel, created_at, updated_at) VALUES
('TXN-1001', 'SAGA-1001', 'IDEMP-9b1deb4d-001', 'US1000000001', 'US2000000002', 250.0000, 'USD', 'COMPLETED', NULL, 'IOS', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
('TXN-1002', 'SAGA-1002', 'IDEMP-9b1deb4d-002', 'US1000000001', 'DE89370400440532013000', 4500.0000, 'EUR', 'COMPLETED', NULL, 'WEB', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
('TXN-1003', 'SAGA-1003', 'IDEMP-9b1deb4d-003', 'US1000000002', 'GB29BARC20041538291044', 1200.0000, 'GBP', 'COMPLETED', NULL, 'ANDROID', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
('TXN-1004', 'SAGA-1004', 'IDEMP-9b1deb4d-004', 'US1000000001', 'IN5000000001', 500.0000, 'USD', 'COMPLETED', NULL, 'WEB', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
('TXN-1005', 'SAGA-1005', 'IDEMP-9b1deb4d-005', 'EU3000000001', 'GB4000000001', 850.0000, 'EUR', 'COMPLETED', NULL, 'IOS', NOW() - INTERVAL '1 days', NOW() - INTERVAL '1 days'),
('TXN-1006', 'SAGA-1006', 'IDEMP-9b1deb4d-006', 'US2000000002', 'US1000000001', 75.5000, 'USD', 'COMPLETED', NULL, 'UPI', NOW() - INTERVAL '6 hours', NOW() - INTERVAL '6 hours'),
('TXN-1007', 'SAGA-1007', 'IDEMP-9b1deb4d-007', 'US1000000001', 'US9999999999', 15000.0000, 'USD', 'REJECTED', 'INSUFFICIENT_FUNDS_OR_LIMIT_EXCEEDED', 'WEB', NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour');

INSERT INTO outbox_events (id, aggregate_type, aggregate_id, event_type, topic, payload, status, retry_count, created_at, published_at) VALUES
('OB-001', 'TRANSFER', 'TXN-1001', 'TransferCompletedEvent', 'banking.payments.transfer.completed', '{"transferId":"TXN-1001","source":"US1000000001","target":"US2000000002","amount":250.00,"currency":"USD"}', 'PUBLISHED', 0, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
('OB-002', 'TRANSFER', 'TXN-1002', 'CrossBorderTransferCompletedEvent', 'banking.payments.transfer.completed', '{"transferId":"TXN-1002","source":"US1000000001","target":"DE89370400440532013000","amount":4500.00,"currency":"EUR"}', 'PUBLISHED', 0, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
('OB-003', 'TRANSFER', 'TXN-1006', 'UpiTransferCompletedEvent', 'banking.payments.transfer.completed', '{"transferId":"TXN-1006","source":"US2000000002","target":"US1000000001","amount":75.50,"currency":"USD"}', 'PUBLISHED', 0, NOW() - INTERVAL '6 hours', NOW() - INTERVAL '6 hours');
