-- V3__add_card_controls_and_rewards.sql
-- Mobile Banking Channel Controls & Loyalty Rewards Schema

ALTER TABLE cards ADD COLUMN IF NOT EXISTS is_online_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE cards ADD COLUMN IF NOT EXISTS is_atm_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE cards ADD COLUMN IF NOT EXISTS is_pos_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE cards ADD COLUMN IF NOT EXISTS reward_points BIGINT NOT NULL DEFAULT 0;

-- Update existing cards with starting reward points
UPDATE cards SET reward_points = 1250 WHERE reward_points = 0;
