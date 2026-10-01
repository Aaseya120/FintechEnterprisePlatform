-- Flyway Migration V1: Customer Multi-Channel Notification Audit Schema

CREATE TABLE notification_logs (
    id VARCHAR(36) NOT NULL,
    recipient VARCHAR(100) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SENT',
    provider_reference VARCHAR(100),
    sent_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_notif_logs PRIMARY KEY (id)
);

CREATE INDEX idx_notif_recipient_channel ON notification_logs (recipient, channel);
CREATE INDEX idx_notif_sent_at ON notification_logs (sent_at DESC);
