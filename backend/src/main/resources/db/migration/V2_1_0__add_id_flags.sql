-- alter existing table

ALTER TABLE forms
    ADD COLUMN bund_id_enabled   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN bund_id_level     INTEGER NULL,
    ADD COLUMN bayern_id_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN bayern_id_level   INTEGER NULL,
    ADD COLUMN muk_enabled       BOOLEAN NOT NULL DEFAULT FALSE;
