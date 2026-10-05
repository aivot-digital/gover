-- add column for internal forms to forms table
ALTER TABLE forms
    ADD COLUMN type smallint not null default 0;
