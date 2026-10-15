ALTER TABLE user_profiles ADD COLUMN onboarding_completed_at DATETIME(6) NULL;
-- Preserve every acceptance/withdrawal cycle, including re-accepting the same version.
-- Create the replacement FK-supporting index before removing the original unique index.
CREATE INDEX ix_user_consents_history ON user_consents (user_id, consent_type, consented_at);
ALTER TABLE user_consents DROP INDEX uq_user_consents_version;
