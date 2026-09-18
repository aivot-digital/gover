-- alter existing tables

ALTER TABLE submissions
    ADD COLUMN is_test_submission BOOLEAN NOT NULL DEFAULT true;
