-- V5__add_saving_vaults.sql
-- Saving Vaults, Saving Goals & Target Sub-Account Pots Schema

CREATE TABLE IF NOT EXISTS saving_vaults (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    parent_account_number VARCHAR(34) NOT NULL,
    vault_name VARCHAR(100) NOT NULL,
    target_amount NUMERIC(19, 4) NOT NULL,
    current_balance NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(3) NOT NULL,
    target_date DATE,
    lock_status VARCHAR(20) NOT NULL DEFAULT 'UNLOCKED',
    auto_roundup_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_vault_customer ON saving_vaults (customer_id);
CREATE INDEX IF NOT EXISTS idx_vault_parent_acc ON saving_vaults (parent_account_number);

-- Seed sample saving vault
INSERT INTO saving_vaults (id, customer_id, parent_account_number, vault_name, target_amount, current_balance, currency, target_date, lock_status, auto_roundup_enabled, created_at, updated_at)
VALUES
    ('vlt_emerg_001', 'cust_001', 'ACC1000000001', 'Rainy Day Emergency Fund', 5000.00, 1500.00, 'USD', CURRENT_DATE + INTERVAL '180 days', 'UNLOCKED', TRUE, NOW() - INTERVAL '30 days', NOW()),
    ('vlt_vacation_002', 'cust_002', 'ACC1000000003', 'Summer Vacation 2027', 3500.00, 850.00, 'USD', CURRENT_DATE + INTERVAL '300 days', 'UNLOCKED', FALSE, NOW() - INTERVAL '45 days', NOW())
ON CONFLICT (id) DO NOTHING;
