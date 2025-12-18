-- TODO: fix this!

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
