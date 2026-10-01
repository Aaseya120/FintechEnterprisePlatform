-- V3__add_standing_instructions.sql
-- Enterprise Standing Instructions & Recurring Auto-Debits Schema

CREATE TABLE IF NOT EXISTS standing_instructions (
    id VARCHAR(36) PRIMARY KEY,
    instruction_name VARCHAR(100) NOT NULL,
    customer_id VARCHAR(36) NOT NULL,
    source_account_number VARCHAR(34) NOT NULL,
    target_account_number VARCHAR(34) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    execution_day INT,
    next_execution_date DATE NOT NULL,
    category VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    total_executions_count INT NOT NULL DEFAULT 0,
    last_executed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_si_customer ON standing_instructions (customer_id, status);
CREATE INDEX IF NOT EXISTS idx_si_next_exec ON standing_instructions (next_execution_date, status);

-- Seed sample standing instructions
INSERT INTO standing_instructions (id, instruction_name, customer_id, source_account_number, target_account_number, amount, currency, frequency, execution_day, next_execution_date, category, status, total_executions_count, created_at)
VALUES
    ('si_rent_001', 'Monthly Apartment Rent', 'cust_001', 'ACC1000000001', 'ACC2000000002', 2500.00, 'USD', 'MONTHLY', 1, CURRENT_DATE + INTERVAL '5 days', 'RENT', 'ACTIVE', 3, NOW() - INTERVAL '3 months'),
    ('si_emi_002', 'Auto Loan Monthly EMI', 'cust_002', 'ACC1000000003', 'ACC9999999999', 450.00, 'USD', 'MONTHLY', 10, CURRENT_DATE + INTERVAL '10 days', 'LOAN_EMI', 'ACTIVE', 6, NOW() - INTERVAL '6 months')
ON CONFLICT (id) DO NOTHING;
