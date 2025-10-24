-- drop existing search view
DROP VIEW IF EXISTS search_items;

-- recreate search view
CREATE VIEW search_items AS
SELECT text 'assets'                   AS origin_table,
       filename                        AS label,
       key::varchar                    AS id,
       filename                        AS search_text,
       to_tsvector('german', filename) AS search_vector
FROM assets
UNION ALL
SELECT text 'data_object_items'                                 AS origin_table,
       id                                                       AS label,
       schema_key || ',' || id                                  AS id,
       concat(id::varchar, ' ', data::varchar)                  AS search_text,
       to_tsvector('german', id) || to_tsvector('german', data) AS search_vector
FROM data_object_items
UNION ALL
SELECT text 'data_object_schemas'  AS origin_table,
       name                        AS label,
       key::varchar                AS id,
       name                        AS search_text,
       to_tsvector('german', name) AS search_vector
FROM data_object_schemas
UNION ALL
SELECT text 'departments'          AS origin_table,
       name                        AS label,
       id::varchar                 AS id,
       name                        AS search_text,
       to_tsvector('german', name) AS search_vector
FROM departments
UNION ALL
SELECT text 'destinations'         AS origin_table,
       name                        AS label,
       id::varchar                 AS id,
       name                        AS search_text,
       to_tsvector('german', name) AS search_vector
FROM destinations
UNION ALL
SELECT text 'forms'                                                                 AS origin_table,
       internal_title || ' (Version ' || version || ')'                             AS label,
       concat(id::varchar, ',', version::varchar)                                   AS id,
       concat(internal_title, ' ', public_title)                                    AS search_text,
       to_tsvector('german', internal_title) || to_tsvector('german', public_title) AS search_vector
FROM form_version_with_details
UNION ALL
SELECT text 'identity_providers'   AS origin_table,
       name                        AS label,
       key::varchar                AS id,
       name                        AS search_text,
       to_tsvector('german', name) AS search_vector
FROM identity_providers
UNION ALL
SELECT text 'payment_providers'    AS origin_table,
       name                        AS label,
       key::varchar                AS id,
       name                        AS search_text,
       to_tsvector('german', name) AS search_vector
FROM payment_providers
UNION ALL
SELECT text 'presets'                            AS origin_table,
       concat(title, ' (Version ', version, ')') AS label,
       concat(preset_key, ',', version::varchar) AS id,
       title                                     AS search_text,
       to_tsvector('german', title)              AS search_vector
FROM preset_version_with_details
UNION ALL
SELECT text 'provider_links'       AS origin_table,
       text                        AS label,
       id::varchar                 AS id,
       text                        AS search_text,
       to_tsvector('german', text) AS search_vector
FROM provider_links
UNION ALL
SELECT text 'secrets'              AS origin_table,
       name                        AS label,
       key::varchar                AS id,
       name                        AS search_text,
       to_tsvector('german', name) AS search_vector
FROM secrets
UNION ALL
SELECT text 'submissions'                                          AS origin_table,
       file_number                                                 AS label,
       id::varchar                                                 AS id,
       concat(id::varchar, ' ', file_number)                       AS search_text,
       to_tsvector('german', id) || to_tsvector('german', COALESCE(file_number, '')) AS search_vector
FROM submissions
UNION ALL
SELECT text 'themes'               AS origin_table,
       name                        AS label,
       id::varchar                 AS id,
       name                        AS search_text,
       to_tsvector('german', name) AS search_vector
FROM themes;
