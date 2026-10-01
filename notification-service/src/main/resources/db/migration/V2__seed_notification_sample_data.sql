-- Flyway Migration V2: Seed Production-Grade Multi-Channel Notification Audit Sample Data

INSERT INTO notification_logs (id, recipient, channel, event_type, subject, content, status, provider_reference, sent_at) VALUES
('NOTIF-001', 'a.hamilton@banking-domain.com', 'EMAIL', 'TRANSFER_COMPLETED', 'Transfer Confirmation: $250.00 Sent Successfully', 'Dear Alexander, your payment of $250.00 to account US2000000002 has settled.', 'SENT', 'SENDGRID_MSG_99182', NOW() - INTERVAL '5 days'),
('NOTIF-002', '+12125550190', 'SMS', 'TRANSFER_COMPLETED', 'Banking Alert: Fund Debit', 'Debit Alert: $250.00 debited from acc US1000000001. Avail Bal: $25,450.75.', 'DELIVERED', 'TWILIO_SM_819204', NOW() - INTERVAL '5 days'),
('NOTIF-003', 'DEVICE_TOKEN_IOS_991', 'PUSH', 'CROSS_BORDER_TRANSFER', 'International Remittance Sent', 'Your transfer of €4,500.00 to Julian Schmidt has been dispatched via SWIFT/SEPA.', 'DELIVERED', 'APNS_ID_0049182', NOW() - INTERVAL '4 days'),
('NOTIF-004', '+12125550190', 'SMS', 'FRAUD_BLOCK_ALERT', 'URGENT: Suspicious Transaction Blocked', 'High-Risk Alert: Attempted transfer of $15,000.00 was blocked. Contact support immediately.', 'DELIVERED', 'TWILIO_SM_994821', NOW() - INTERVAL '1 hour');
