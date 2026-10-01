-- V3__add_mobile_device_registrations.sql
-- Mobile Banking Push Notification Tokens & Device Binding Schema (APNs / FCM)

CREATE TABLE IF NOT EXISTS mobile_device_registrations (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    device_token VARCHAR(512) NOT NULL,
    device_model VARCHAR(100),
    os_version VARCHAR(50),
    app_version VARCHAR(50),
    biometric_key VARCHAR(512),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    registered_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_active_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_mobile_cust_active ON mobile_device_registrations (customer_id, is_active);
CREATE INDEX IF NOT EXISTS idx_mobile_token ON mobile_device_registrations (device_token);

-- Seed sample iOS and Android mobile device registrations
INSERT INTO mobile_device_registrations (id, customer_id, platform, device_token, device_model, os_version, app_version, biometric_key, is_active, registered_at, last_active_at)
VALUES
    ('dev_ios_001', 'cust_001', 'IOS', 'apns_token_98374982734982734982374982374982374982374982374982734987', 'iPhone 15 Pro Max', 'iOS 18.2', 'v2.4.0', 'pk_secp256r1_ios_key_01', TRUE, NOW() - INTERVAL '15 days', NOW()),
    ('dev_and_002', 'cust_002', 'ANDROID', 'fcm_token_e9018239018230918203918203918203918203918203918203918203', 'Google Pixel 8 Pro', 'Android 14', 'v2.4.0', 'pk_secp256r1_and_key_02', TRUE, NOW() - INTERVAL '30 days', NOW())
ON CONFLICT (id) DO NOTHING;
