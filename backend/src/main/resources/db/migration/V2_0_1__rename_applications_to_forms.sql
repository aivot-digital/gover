-- alter existing tables

ALTER TABLE submissions
    RENAME COLUMN application_id TO form_id;
