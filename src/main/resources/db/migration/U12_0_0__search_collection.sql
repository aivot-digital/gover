-- Drop the view
DROP VIEW IF EXISTS search_items;

-- Drop all created indexes
DROP INDEX IF EXISTS idx_assets_filename_full_text;
DROP INDEX IF EXISTS idx_data_object_items_id_full_text;
DROP INDEX IF EXISTS idx_data_object_schemAS_name_full_text;
DROP INDEX IF EXISTS idx_departments_name_full_text;
DROP INDEX IF EXISTS idx_destinations_name_full_text;
DROP INDEX IF EXISTS idx_forms_internal_title_full_text;
DROP INDEX IF EXISTS idx_identity_providers_name_full_text;
DROP INDEX IF EXISTS idx_payment_providers_name_full_text;
DROP INDEX IF EXISTS idx_presets_title_full_text;
DROP INDEX IF EXISTS idx_provider_links_text_full_text;
DROP INDEX IF EXISTS idx_secrets_name_full_text;
DROP INDEX IF EXISTS idx_submissions_file_number_full_text;
DROP INDEX IF EXISTS idx_themes_name_full_text;

-- Restore the filename column in assets (if possible)
UPDATE assets
SET filename = filename || '.' || extension
WHERE extension IS NOT NULL;

-- Remove the extension column
ALTER TABLE assets
DROP COLUMN IF EXISTS extension;