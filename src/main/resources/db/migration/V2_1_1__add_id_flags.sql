-- alter existing table

ALTER TABLE forms
    ADD COLUMN sh_id_enabled   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN sh_id_level     INTEGER NULL;
