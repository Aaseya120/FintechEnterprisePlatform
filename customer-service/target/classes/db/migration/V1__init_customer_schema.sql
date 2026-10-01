-- Flyway Migration V1: Customer Onboarding, KYC, and Beneficiary Management

CREATE TABLE customers (
    id VARCHAR(36) NOT NULL,
    customer_number VARCHAR(20) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    date_of_birth DATE NOT NULL,
    address VARCHAR(255) NOT NULL,
    risk_category VARCHAR(20) NOT NULL DEFAULT 'LOW',
    status VARCHAR(20) NOT NULL DEFAULT 'ONBOARDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uk_customers_num UNIQUE (customer_number),
    CONSTRAINT uk_customers_email UNIQUE (email)
);

CREATE INDEX idx_customers_email_status ON customers (email, status);
CREATE INDEX idx_customers_phone ON customers (phone);

CREATE TABLE customer_kyc (
    id VARCHAR(36) NOT NULL,
    customer_id VARCHAR(36) NOT NULL,
    id_type VARCHAR(30) NOT NULL,
    id_number VARCHAR(50) NOT NULL,
    document_url VARCHAR(255) NOT NULL,
    verification_status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    rejection_reason VARCHAR(255),
    verified_by VARCHAR(50),
    verified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_customer_kyc PRIMARY KEY (id),
    CONSTRAINT fk_kyc_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE INDEX idx_kyc_customer_status ON customer_kyc (customer_id, verification_status);

CREATE TABLE beneficiaries (
    id VARCHAR(36) NOT NULL,
    customer_id VARCHAR(36) NOT NULL,
    beneficiary_name VARCHAR(100) NOT NULL,
    account_number VARCHAR(34) NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    routing_or_ifsc_code VARCHAR(20) NOT NULL,
    beneficiary_type VARCHAR(20) NOT NULL,
    max_transfer_limit NUMERIC(19, 4) NOT NULL,
    cooling_end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_beneficiaries PRIMARY KEY (id),
    CONSTRAINT fk_beneficiary_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE INDEX idx_beneficiary_cust_status ON beneficiaries (customer_id, is_active);
