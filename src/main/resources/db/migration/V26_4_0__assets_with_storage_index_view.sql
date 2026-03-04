-- Shift asset read model to storage-index-backed fields.
-- Keep key for public access, but use provider/path reference as canonical identity for staff access.

ALTER TABLE assets
    DROP COLUMN IF EXISTS filename,
    DROP COLUMN IF EXISTS content_type;

ALTER TABLE assets
    ADD CONSTRAINT assets_storage_provider_path_unique
        UNIQUE (storage_provider_id, storage_path_from_root);

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
