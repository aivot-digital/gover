-- alter existing table

ALTER TABLE submissions
    ADD COLUMN payment_originator_id VARCHAR(36) NULL,
    ADD COLUMN payment_endpoint_id VARCHAR(36) NULL;