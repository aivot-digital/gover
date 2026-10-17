-- alter existing table

ALTER TABLE forms
    ADD COLUMN products jsonb NULL,
    ADD COLUMN payment_purpose VARCHAR(27) NULL,
    ADD COLUMN payment_originator_id VARCHAR(36) NULL,
    ADD COLUMN payment_endpoint_id VARCHAR(36) NULL,
    ADD COLUMN payment_provider VARCHAR(16) NULL;

ALTER TABLE submissions
    ADD COLUMN status INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN payment_status INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN payment_request jsonb NULL,
    ADD COLUMN payment_information jsonb NULL;

-- update status for existing submissions
UPDATE submissions SET status = 1 WHERE assignee_id IS NOT NULL;
UPDATE submissions SET status = 999 WHERE archived IS NOT NULL;
