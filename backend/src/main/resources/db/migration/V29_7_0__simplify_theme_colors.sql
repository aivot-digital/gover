ALTER TABLE themes
    ADD COLUMN primary_color        VARCHAR(7) NOT NULL DEFAULT '#733635',
    ADD COLUMN secondary_color      VARCHAR(7) NOT NULL DEFAULT '#A0C9CB',
    ADD COLUMN primary_color_dark   VARCHAR(7),
    ADD COLUMN secondary_color_dark VARCHAR(7),
    ADD COLUMN logo_key_dark        UUID REFERENCES assets (key) ON DELETE SET NULL;

ALTER TABLE themes
    ALTER COLUMN primary_color DROP DEFAULT,
    ALTER COLUMN secondary_color DROP DEFAULT,
    DROP COLUMN main,
    DROP COLUMN main_dark,
    DROP COLUMN accent,
    DROP COLUMN error,
    DROP COLUMN warning,
    DROP COLUMN info,
    DROP COLUMN success;
