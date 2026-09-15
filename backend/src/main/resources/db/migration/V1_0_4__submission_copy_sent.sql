-- alter existing tables

ALTER TABLE submissions
    ADD COLUMN copy_sent  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN copy_tries INTEGER NOT NULL DEFAULT 0;
