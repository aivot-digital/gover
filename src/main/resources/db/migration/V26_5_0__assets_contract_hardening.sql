-- Finalize assets DB contract for provider/path identity.
-- Keep key for public access, but enforce strict document-path constraints.

DO
$$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM assets
        GROUP BY storage_provider_id, storage_path_from_root
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot enforce unique asset provider/path identity: duplicate rows exist in assets.';
    END IF;
END
$$;

DO
$$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'assets_storage_provider_path_unique'
    ) THEN
        ALTER TABLE assets
            ADD CONSTRAINT assets_storage_provider_path_unique
                UNIQUE (storage_provider_id, storage_path_from_root);
    END IF;
END
$$;

DO
$$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'assets_storage_path_not_blank'
    ) THEN
        ALTER TABLE assets
            ADD CONSTRAINT assets_storage_path_not_blank
                CHECK (length(btrim(storage_path_from_root)) > 0);
    END IF;
END
$$;

DO
$$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'assets_storage_path_absolute'
    ) THEN
        ALTER TABLE assets
            ADD CONSTRAINT assets_storage_path_absolute
                CHECK (left(storage_path_from_root, 1) = '/');
    END IF;
END
$$;

DO
$$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'assets_storage_path_is_file'
    ) THEN
        ALTER TABLE assets
            ADD CONSTRAINT assets_storage_path_is_file
                CHECK (storage_path_from_root <> '/' AND right(storage_path_from_root, 1) <> '/');
    END IF;
END
$$;

DO
$$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'assets_storage_path_length'
    ) THEN
        ALTER TABLE assets
            ADD CONSTRAINT assets_storage_path_length
                CHECK (length(storage_path_from_root) <= 2048);
    END IF;
END
$$;

CREATE OR REPLACE VIEW assets_with_metadata AS
SELECT
    a.key,
    a.created,
    a.uploader_id,
    a.is_private,
    a.storage_provider_id,
    a.storage_path_from_root,
    sii.filename,
    sii.mime_type AS content_type,
    COALESCE(sii.metadata, '{}'::jsonb) AS metadata
FROM assets a
INNER JOIN storage_index_items sii
    ON sii.storage_provider_id = a.storage_provider_id
    AND sii.path_from_root = a.storage_path_from_root
    AND sii.directory = FALSE;
