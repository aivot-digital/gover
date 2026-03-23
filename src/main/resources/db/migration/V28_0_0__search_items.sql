CREATE VIEW v_search_items AS

-- Assets
SELECT text 'assets'                                                  AS origin_table,
       filename                                                       AS label,
       storage_provider_id::varchar || ',' || path_from_root::varchar AS id,
       to_tsvector('german', filename)                                AS searchable_element,
       filename                                                       AS search_text
FROM v_storage_index_items_with_assets

UNION ALL

-- Data Object Items
SELECT text 'data_object_items'                                 AS origin_table,
       id                                                       AS label,
       schema_key || ',' || id                                  AS id,
       to_tsvector('german', id) || to_tsvector('german', data) AS searchable_element,
       id || ' ' || data::varchar                               AS search_text
FROM data_object_items

UNION ALL

-- Data Object Schemas
SELECT text 'data_object_schemas'  AS origin_table,
       name                        AS label,
       key::varchar                AS id,
       to_tsvector('german', name) AS searchable_element,
       name                        AS search_text
FROM data_object_schemas

UNION ALL

-- Departments
SELECT text 'departments'          AS origin_table,
       name                        AS label,
       id::varchar                 AS id,
       to_tsvector('german', name) AS searchable_element,
       name                        AS search_text
FROM departments

UNION ALL

-- Teams
SELECT text 'teams'                AS origin_table,
       name                        AS label,
       id::varchar                 AS id,
       to_tsvector('german', name) AS searchable_element,
       name                        AS search_text
FROM teams

UNION ALL

-- Forms
SELECT text 'forms'                                     AS origin_table,
       internal_title || ' (Version ' || version || ')' AS label,
       id::varchar || ',' || version::varchar           AS id,
       to_tsvector('german', internal_title)            AS searchable_element,
       internal_title                                   AS search_text
FROM form_version_with_details

UNION ALL

-- Identity Providers
SELECT text 'identity_providers'   AS origin_table,
       name                        AS label,
       key::varchar                AS id,
       to_tsvector('german', name) AS searchable_element,
       name                        AS search_text
FROM identity_providers

UNION ALL

-- Payment Providers
SELECT text 'payment_providers'    AS origin_table,
       name                        AS label,
       key::varchar                AS id,
       to_tsvector('german', name) AS searchable_element,
       name                        AS search_text
FROM payment_providers

UNION ALL

-- Storage Providers
SELECT text 'storage_providers'    AS origin_table,
       name                        AS label,
       id::varchar                 AS id,
       to_tsvector('german', name) AS searchable_element,
       name                        AS search_text
FROM storage_providers

UNION ALL

-- Presets
SELECT text 'presets'                          AS origin_table,
       title || ' (Version ' || version || ')' AS label,
       preset_key || ',' || version::varchar   AS id,
       to_tsvector('german', title)            AS searchable_element,
       title                                   AS search_text
FROM preset_version_with_details

UNION ALL

-- Provider Links
SELECT text 'provider_links'       AS origin_table,
       text                        AS label,
       id::varchar                 AS id,
       to_tsvector('german', text) AS searchable_element,
       text                        AS search_text
FROM provider_links

UNION ALL

-- Secrets
SELECT text 'secrets'              AS origin_table,
       name                        AS label,
       key::varchar                AS id,
       to_tsvector('german', name) AS searchable_element,
       name                        AS search_text
FROM secrets

UNION ALL

-- Themes
SELECT text 'themes'               AS origin_table,
       name                        AS label,
       id::varchar                 AS id,
       to_tsvector('german', name) AS searchable_element,
       name                        AS search_text
FROM themes

UNION ALL

-- Domain Roles
SELECT text 'domain_roles'         AS origin_table,
       name                        AS label,
       id::varchar                 AS id,
       to_tsvector('german', name) AS searchable_element,
       name                        AS search_text
FROM domain_roles

UNION ALL

-- System Roles
SELECT text 'system_roles'         AS origin_table,
       name                        AS label,
       id::varchar                 AS id,
       to_tsvector('german', name) AS searchable_element,
       name                        AS search_text
FROM system_roles

UNION ALL

-- Process Versions
SELECT text 'processes'                                              AS origin_table,
       p.internal_title || ' (Version ' || pv.process_version || ')' AS label,
       p.id::varchar || ',' || pv.process_version                    AS id,
       to_tsvector('german', p.internal_title)                       AS searchable_element,
       p.internal_title                                              AS search_text
FROM process_versions pv
         JOIN processes p ON pv.process_id = p.id

UNION ALL

-- Process Instances
SELECT text 'process_instances'                                                         AS origin_table,
       pi.id::varchar || ' (' || array_to_string(pi.assigned_file_numbers, ', ') || ')' AS label,
       pi.id::varchar                                                                   AS id,
       to_tsvector('german', pi.id::varchar) ||
       to_tsvector('german', array_to_string(pi.assigned_file_numbers, ', '))           AS searchable_element,
       pi.id::varchar || ' ' || array_to_string(pi.assigned_file_numbers, ', ')         AS search_text
FROM process_instances pi;