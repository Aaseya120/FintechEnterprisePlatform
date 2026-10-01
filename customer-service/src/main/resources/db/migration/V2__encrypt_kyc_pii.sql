-- Flyway Migration V2: Expand KYC ID Number column to support AES-256-GCM encrypted ciphertext

ALTER TABLE customer_kyc ALTER COLUMN id_number TYPE VARCHAR(255);
