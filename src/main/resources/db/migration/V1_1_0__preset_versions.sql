-- alter columns for presets

ALTER TABLE presets
    ADD COLUMN title VARCHAR(190) NOT NULL DEFAULT 'Ubenannte Vorlage',
    ADD COLUMN key uuid NOT NULL DEFAULT gen_random_uuid(),
    ADD COLUMN store_id uuid NULL,

    ADD COLUMN current_version varchar(11) NULL,
    ADD COLUMN current_published_version varchar(11) NULL,
    ADD COLUMN current_store_version varchar(11) NULL,

    DROP CONSTRAINT presets_pkey,
    DROP COLUMN id,

    ADD PRIMARY KEY (key);

-- create new tables

CREATE TABLE preset_versions
(
    preset  uuid        NOT NULL REFERENCES presets ON DELETE CASCADE,
    version VARCHAR(11) NOT NULL DEFAULT '1.0.0',
    root    jsonb       NOT NULL DEFAULT '{}',

    published_at timestamp NULL,
    published_store_at timestamp NULL,

    created TIMESTAMP   NOT NULL,
    updated TIMESTAMP   NOT NULL,

    PRIMARY KEY (preset, version)
);

-- transfer data

INSERT INTO preset_versions (preset, version, root, published_at, published_store_at, created, updated)
    SELECT key, '1.0.0', root, current_timestamp, null, created, updated FROM presets;

UPDATE presets
    SET current_version = '1.0.0',
        current_published_version = '1.0.0',
        current_store_version = null;

-- alter columns for presets

ALTER TABLE presets
    DROP COLUMN root;
