-- Flyway Migration V1: Debit and Credit Card Management Schema

CREATE TABLE cards (
    id VARCHAR(36) NOT NULL,
    card_number VARCHAR(19) NOT NULL,
    card_network VARCHAR(20) NOT NULL,
    card_type VARCHAR(20) NOT NULL,
    customer_id VARCHAR(36) NOT NULL,
    linked_account_number VARCHAR(34) NOT NULL,
    card_holder_name VARCHAR(100) NOT NULL,
    expiry_month INT NOT NULL,
    expiry_year INT NOT NULL,
    cvv_hash VARCHAR(64) NOT NULL,
    pin_hash VARCHAR(64),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    daily_limit NUMERIC(19, 4) NOT NULL DEFAULT 5000.00,
    is_international_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    is_contactless_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_cards PRIMARY KEY (id),
    CONSTRAINT uk_cards_number UNIQUE (card_number)
);

CREATE INDEX idx_cards_customer_status ON cards (customer_id, status);
CREATE INDEX idx_cards_account ON cards (linked_account_number);
