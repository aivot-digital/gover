DROP VIEW v_search_items;

CREATE VIEW v_search_items AS

-- Assets
SELECT text 'assets'                                                  AS origin_table,
       null                                                           AS origin_table_subset,
       filename                                                       AS label,
       storage_provider_id::varchar || ',' || path_from_root::varchar AS id,
       to_tsvector('german', filename)                                AS searchable_element,
       filename                                                       AS search_text,
       usp.user_id                                                    AS user_id,
       usp.permissions                                                AS permissions
FROM v_storage_index_items_with_assets
         CROSS JOIN v_user_system_permission AS usp
WHERE directory = false
  AND missing = false

UNION ALL

-- Data Object Items
SELECT text 'data_object_items'                                 AS origin_table,
       null                                                     AS origin_table_subset,
       id                                                       AS label,
       schema_key || ',' || id                                  AS id,
       to_tsvector('german', id) || to_tsvector('german', data) AS searchable_element,
       id || ' ' || data::varchar                               AS search_text,
       usp.user_id                                              AS user_id,
       usp.permissions                                          AS permissions
FROM data_object_items
         CROSS JOIN v_user_system_permission AS usp
UNION ALL

-- Data Object Schemas
SELECT text 'data_object_schemas'  AS origin_table,
       null                        AS origin_table_subset,
       name                        AS label,
       key::varchar                AS id,
       to_tsvector('german', name) AS searchable_element,
       name                        AS search_text,
       usp.user_id                 AS user_id,
       usp.permissions             AS permissions
FROM data_object_schemas
         CROSS JOIN v_user_system_permission AS usp
UNION ALL

-- Departments
SELECT text 'departments'          AS origin_table,
       null                        AS origin_table_subset,
       name                        AS label,
       id::varchar                 AS id,
       to_tsvector('german', name) AS searchable_element,
       name                        AS search_text,
       udp.user_id                 AS user_id,
       udp.permissions             AS permissions
FROM departments
         JOIN v_user_department_permissions AS udp
              ON udp.department_id = departments.id

UNION ALL

-- Teams
SELECT text 'teams'                AS origin_table,
       null                        AS origin_table_subset,
       name                        AS label,
       id::varchar                 AS id,
       to_tsvector('german', name) AS searchable_element,
       name                        AS search_text,
       utp.user_id                 AS user_id,
       utp.permissions             AS permissions
FROM teams
         JOIN v_user_team_permissions AS utp
              ON utp.team_id = teams.id

UNION ALL

-- Users
SELECT text 'users'                                                              AS origin_table,
       null                                                                      AS origin_table_subset,
       coalesce(nullif(full_name, ''), email, id)                                AS label,
       id::varchar                                                               AS id,
       to_tsvector('german', coalesce(full_name, '')) ||
       to_tsvector('german', coalesce(email, ''))                                AS searchable_element,
       trim(coalesce(full_name, '') || ' ' || coalesce(email, ''))               AS search_text,
       usp.user_id                                                               AS user_id,
       usp.permissions                                                           AS permissions
FROM users
         CROSS JOIN v_user_system_permission AS usp
WHERE deleted_in_idp = false

UNION ALL

-- Process Nodes
SELECT text 'process_nodes'                                         AS origin_table,
       process_node_definition_key::varchar                         AS origin_table_subset,
       coalesce(name, data_key, '')                                 AS label,
       id::varchar                                                  AS id,
       to_tsvector('german', coalesce(name, '')) ||
       to_tsvector('german', coalesce(description, ''))             AS searchable_element,
       trim(coalesce(name, '') || ' ' || coalesce(description, '')) AS search_text,
       upp.user_id                                                  AS user_id,
       upp.permissions                                              AS permissions
FROM process_nodes
         JOIN v_user_process_access_permissions AS upp
              ON upp.target_process_id = process_nodes.process_id -- TODO: Cross Join w/ v_user_system_permission
WHERE coalesce(name, '') <> ''
   OR coalesce(description, '') <> ''

UNION ALL

-- Identity Providers
SELECT text 'identity_providers'   AS origin_table,
       null                        AS origin_table_subset,
       name                        AS label,
       key::varchar                AS id,
       to_tsvector('german', name) AS searchable_element,
       name                        AS search_text,
       usp.user_id                 AS user_id,
       usp.permissions             AS permissions
FROM identity_providers
         CROSS JOIN v_user_system_permission AS usp
UNION ALL

-- Payment Providers
SELECT text 'payment_providers'    AS origin_table,
       null                        AS origin_table_subset,
       name                        AS label,
       key::varchar                AS id,
       to_tsvector('german', name) AS searchable_element,
       name                        AS search_text,
       usp.user_id                 AS user_id,
       usp.permissions             AS permissions
FROM payment_providers
         CROSS JOIN v_user_system_permission AS usp

UNION ALL

-- Storage Providers
SELECT text 'storage_providers'    AS origin_table,
       null                        AS origin_table_subset,
       name                        AS label,
       id::varchar                 AS id,
       to_tsvector('german', name) AS searchable_element,
       name                        AS search_text,
       usp.user_id                 AS user_id,
       usp.permissions             AS permissions
FROM storage_providers
         CROSS JOIN v_user_system_permission AS usp

UNION ALL

-- Presets
SELECT text 'presets'                          AS origin_table,
       null                                    AS origin_table_subset,
       title || ' (Version ' || version || ')' AS label,
       preset_key || ',' || version::varchar   AS id,
       to_tsvector('german', title)            AS searchable_element,
       title                                   AS search_text,
       usp.user_id                             AS user_id,
       usp.permissions                         AS permissions
FROM preset_version_with_details
         CROSS JOIN v_user_system_permission AS usp

UNION ALL

-- Provider Links
SELECT text 'provider_links'       AS origin_table,
       null                        AS origin_table_subset,
       text                        AS label,
       id::varchar                 AS id,
       to_tsvector('german', text) AS searchable_element,
       text                        AS search_text,
       usp.user_id                 AS user_id,
       usp.permissions             AS permissions
FROM provider_links
         CROSS JOIN v_user_system_permission AS usp

UNION ALL

-- Secrets
SELECT text 'secrets'              AS origin_table,
       null                        AS origin_table_subset,
       name                        AS label,
       key::varchar                AS id,
       to_tsvector('german', name) AS searchable_element,
       name                        AS search_text,
       usp.user_id                 AS user_id,
       usp.permissions             AS permissions
FROM secrets
         CROSS JOIN v_user_system_permission AS usp

UNION ALL

-- Themes
SELECT text 'themes'               AS origin_table,
       null                        AS origin_table_subset,
       name                        AS label,
       id::varchar                 AS id,
       to_tsvector('german', name) AS searchable_element,
       name                        AS search_text,
       usp.user_id                 AS user_id,
       usp.permissions             AS permissions
FROM themes
         CROSS JOIN v_user_system_permission AS usp

UNION ALL

-- Code Lists
SELECT text 'code_lists'             AS origin_table,
        null                         AS origin_table_subset,
        name                         AS label,
        key                          AS id,
        to_tsvector('german', name)  AS searchable_element,
        name                         AS search_text,
        usp.user_id                  AS user_id,
        usp.permissions              AS permissions
FROM code_lists
         CROSS JOIN v_user_system_permission AS usp

UNION ALL

-- Domain Roles
SELECT text 'domain_roles'         AS origin_table,
       null                        AS origin_table_subset,
       name                        AS label,
       id::varchar                 AS id,
       to_tsvector('german', name) AS searchable_element,
       name                        AS search_text,
       usp.user_id                 AS user_id,
       usp.permissions             AS permissions
FROM domain_roles
         CROSS JOIN v_user_system_permission AS usp

UNION ALL

-- System Roles
SELECT text 'system_roles'         AS origin_table,
       null                        AS origin_table_subset,
       name                        AS label,
       id::varchar                 AS id,
       to_tsvector('german', name) AS searchable_element,
       name                        AS search_text,
       usp.user_id                 AS user_id,
       usp.permissions             AS permissions
FROM system_roles
         CROSS JOIN v_user_system_permission AS usp

UNION ALL

-- Process Versions
SELECT text 'processes'                                              AS origin_table,
       null                                                          AS origin_table_subset,
       p.internal_title || ' (Version ' || pv.process_version || ')' AS label,
       p.id::varchar || ',' || pv.process_version                    AS id,
       to_tsvector('german', p.internal_title)                       AS searchable_element,
       p.internal_title                                              AS search_text,
       usp.user_id                                                   as user_id,
       usp.permissions                                               as permissions
FROM process_versions pv
         JOIN processes p ON pv.process_id = p.id
         JOIN v_user_process_access_permissions AS usp ON usp.target_process_id = p.id -- TODO: Cross Join w/ v_user_system_permission

UNION ALL

-- Process Instances
SELECT text 'process_instances'                                     AS origin_table,
       null                                                         AS origin_table_subset,
       pi.case_number::varchar || (case
                                       when pi.assigned_file_numbers = '{}' then ''
                                       else (' (' || array_to_string(pi.assigned_file_numbers, ', ') || ')')
           end)                                                     AS label,
       pi.id::varchar                                               AS id,
       to_tsvector('german', pi.case_number::varchar) ||
       to_tsvector('german',
                   array_to_string(pi.assigned_file_numbers, ', ')) AS searchable_element,
       pi.case_number::varchar || ' ' ||
       array_to_string(pi.assigned_file_numbers, ', ')              AS search_text,
       upp.user_id                                                  AS user_id,
       upp.permissions                                              AS permissions
FROM process_instances pi
         JOIN v_user_process_instance_access_permissions AS upp
              ON upp.target_process_instance_id = pi.id; -- TODO: Cross Join w/ v_user_system_permission
