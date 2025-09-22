CREATE INDEX idx_assets_filename_full_text
    ON assets
        USING GIN (to_tsvector('german', filename));

CREATE INDEX idx_data_object_items_id_full_text
    ON data_object_items
        USING GIN (to_tsvector('german', id::text));

CREATE INDEX idx_data_object_items_data_full_text
    ON data_object_items
        USING GIN (to_tsvector('german', data::text));

CREATE INDEX idx_data_object_schemas_name_full_text
    ON data_object_schemas
        USING GIN (to_tsvector('german', name));

CREATE INDEX idx_departments_name_full_text
    ON departments
        USING GIN (to_tsvector('german', name));

CREATE INDEX idx_destinations_name_full_text
    ON destinations
        USING GIN (to_tsvector('german', name));

-- CREATE INDEX idx_forms_internal_title_full_text
--     ON form_version_with_details
--         USING GIN (to_tsvector('german', internal_title));

CREATE INDEX idx_identity_providers_name_full_text
    ON identity_providers
        USING GIN (to_tsvector('german', name));

CREATE INDEX idx_payment_providers_name_full_text
    ON payment_providers
        USING GIN (to_tsvector('german', name));

-- CREATE INDEX idx_preset_version_with_details_title_full_text
--     ON preset_version_with_details
--         USING GIN (to_tsvector('german', title));

CREATE INDEX idx_provider_links_text_full_text
    ON provider_links
        USING GIN (to_tsvector('german', text));

CREATE INDEX idx_secrets_name_full_text
    ON secrets
        USING GIN (to_tsvector('german', name));

CREATE INDEX idx_submissions_file_number_full_text
    ON submissions
        USING GIN (to_tsvector('german', file_number));

CREATE INDEX idx_themes_name_full_text
    ON themes
        USING GIN (to_tsvector('german', name));

CREATE VIEW search_items AS
SELECT text 'assets'                   AS origin_table,
       filename                        AS label,
       key::varchar                    AS id,
       to_tsvector('german', filename) AS searchable_element
FROM assets
UNION ALL
SELECT text 'data_object_items'  AS origin_table,
       id                        AS label,
       schema_key || ',' || id   AS id,
       to_tsvector('german', id) || to_tsvector('german', data) AS searchable_element
FROM data_object_items
UNION ALL
SELECT text 'data_object_schemas'  AS origin_table,
       name                        AS label,
       key::varchar                AS id,
       to_tsvector('german', name) AS searchable_element
FROM data_object_schemas
UNION ALL
SELECT text 'departments'          AS origin_table,
       name                        AS label,
       id::varchar                 AS id,
       to_tsvector('german', name) AS searchable_element
FROM departments
UNION ALL
SELECT text 'destinations'         AS origin_table,
       name                        AS label,
       id::varchar                 AS id,
       to_tsvector('german', name) AS searchable_element
FROM destinations
UNION ALL
SELECT text 'forms'                                     AS origin_table,
       internal_title || ' (Version ' || version || ')' AS label,
       id::varchar || ',' || version::varchar           AS id,
       to_tsvector('german', internal_title)            AS searchable_element
FROM form_version_with_details
UNION ALL
SELECT text 'identity_providers'   AS origin_table,
       name                        AS label,
       key::varchar                AS id,
       to_tsvector('german', name) AS searchable_element
FROM identity_providers
UNION ALL
SELECT text 'payment_providers'    AS origin_table,
       name                        AS label,
       key::varchar                AS id,
       to_tsvector('german', name) AS searchable_element
FROM payment_providers
UNION ALL
SELECT text 'presets'                          AS origin_table,
       title || ' (Version ' || version || ')' AS label,
       preset_key || ',' || version::varchar   AS id,
       to_tsvector('german', title)            AS searchable_element
FROM preset_version_with_details
UNION ALL
SELECT text 'provider_links'       AS origin_table,
       text                        AS label,
       id::varchar                 AS id,
       to_tsvector('german', text) AS searchable_element
FROM provider_links
UNION ALL
SELECT text 'secrets'              AS origin_table,
       name                        AS label,
       key::varchar                AS id,
       to_tsvector('german', name) AS searchable_element
FROM secrets
UNION ALL
SELECT text 'submissions'                 AS origin_table,
       file_number                        AS label,
       id::varchar                        AS id,
       to_tsvector('german', file_number) AS searchable_element
FROM submissions
UNION ALL
SELECT text 'themes'               AS origin_table,
       name                        AS label,
       id::varchar                 AS id,
       to_tsvector('german', name) AS searchable_element
FROM themes;
