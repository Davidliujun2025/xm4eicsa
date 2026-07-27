-- Purpose:
-- Fix legacy databases that were baselined/skipped and are missing customer_service_user.account.
-- Strategy (non-destructive):
-- 1) Add nullable account column if absent
-- 2) Backfill deterministic unique values for existing rows
-- 3) Enforce NOT NULL
-- 4) Ensure unique index exists

ALTER TABLE customer_service_user
    ADD COLUMN IF NOT EXISTS account VARCHAR(32) NULL AFTER id;

UPDATE customer_service_user
SET account = LOWER(CONCAT('legacy_', id))
WHERE account IS NULL OR account = '';

ALTER TABLE customer_service_user
    MODIFY COLUMN account VARCHAR(32) NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_service_user_account
    ON customer_service_user (account);
