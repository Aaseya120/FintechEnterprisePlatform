-- ============================================================================
-- V6: Customer Credentials & Authentication Table
-- Supports secure password hashing (BCrypt), JWT Refresh Token Rotation (RTR),
-- and account lockout on failed login attempts.
-- ============================================================================

CREATE TABLE IF NOT EXISTS customer_credentials (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL UNIQUE,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    refresh_token VARCHAR(255),
    refresh_token_expiry TIMESTAMP WITH TIME ZONE,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_credentials_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_cred_username ON customer_credentials(username);
CREATE INDEX IF NOT EXISTS idx_cred_customer ON customer_credentials(customer_id);
CREATE INDEX IF NOT EXISTS idx_cred_refresh_token ON customer_credentials(refresh_token);
