-- Flyway Migration V4: Expand Customer KYC Schema for Digital Onboarding & Video KYC (V-KYC)
-- Adds Proof of Address (POA), Biometric Liveness, Video/Audio Sessions, and GPS Geolocation

ALTER TABLE customer_kyc
    ADD COLUMN IF NOT EXISTS address_proof_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS address_proof_url VARCHAR(255),
    ADD COLUMN IF NOT EXISTS selfie_url VARCHAR(255),
    ADD COLUMN IF NOT EXISTS liveness_score DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS liveness_status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS video_kyc_url VARCHAR(255),
    ADD COLUMN IF NOT EXISTS audio_sample_url VARCHAR(255),
    ADD COLUMN IF NOT EXISTS geo_latitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS geo_longitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS ocr_extracted_data VARCHAR(1000);
