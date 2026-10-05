-- alter existing tables

ALTER TABLE assets
    ADD COLUMN content_type VARCHAR(255) NULL;