-- alter existing table

ALTER TABLE submissions
    DROP COLUMN payment_status,
    ADD COLUMN payment_provider VARCHAR(255) NULL,
    ADD COLUMN updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE submission_attachments
    ADD COLUMN content_type VARCHAR(255) NULL;