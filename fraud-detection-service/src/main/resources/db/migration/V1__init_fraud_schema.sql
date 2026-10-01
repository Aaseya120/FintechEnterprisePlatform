-- Flyway Migration V1: Fraud Detection and Risk Management Schema

CREATE TABLE fraud_alerts (
    id VARCHAR(36) NOT NULL,
    transaction_id VARCHAR(64) NOT NULL,
    account_number VARCHAR(34) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    risk_score INT NOT NULL,
    risk_decision VARCHAR(30) NOT NULL,
    triggered_rules TEXT NOT NULL,
    client_ip VARCHAR(45),
    location VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_fraud_alerts PRIMARY KEY (id)
);

CREATE INDEX idx_fraud_acc_created ON fraud_alerts (account_number, created_at DESC);
CREATE INDEX idx_fraud_decision ON fraud_alerts (risk_decision);
