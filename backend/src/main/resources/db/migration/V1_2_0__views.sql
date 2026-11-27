create view preset_version_with_details(key, title, published_version, drafted_version, version_count, preset_key, root_element, created, updated, version, status, published, revoked) as
SELECT prs.key,
       prs.title,
       prs.published_version,
       prs.drafted_version,
       prs.version_count,
       vers.preset_key,
       vers.root_element,
       vers.created,
       vers.updated,
       vers.version,
       vers.status,
       vers.published,
       vers.revoked
FROM preset_versions vers
         JOIN presets prs ON prs.key = vers.preset_key;

create view v_departments_shadowed(id, name, postal_address, imprint, common_privacy, common_accessibility, technical_support_email, special_support_email, created, updated, theme_id, technical_support_phone, technical_support_info, special_support_phone, special_support_info, default_mail_signature, depth, parent_department_id, parent_names, parent_ids) as
WITH RECURSIVE departments_cte AS (SELECT aou.id,
                                          aou.name,
                                          aou.postal_address,
                                          aou.imprint,
                                          aou.common_privacy,
                                          aou.common_accessibility,
                                          aou.technical_support_email,
                                          aou.special_support_email,
                                          aou.created,
                                          aou.updated,
                                          aou.theme_id,
                                          aou.technical_support_phone,
                                          aou.technical_support_info,
                                          aou.special_support_phone,
                                          aou.special_support_info,
                                          aou.default_mail_signature,
                                          aou.depth,
                                          aou.parent_department_id,
                                          '{}'::character varying[] AS parent_names,
        '{}'::integer[]           AS parent_ids
        FROM departments aou
        WHERE aou.parent_department_id IS NULL
        UNION ALL
SELECT rou.id,
       rou.name,
       COALESCE(rou.postal_address, p.postal_address)                   AS postal_address,
       COALESCE(rou.imprint, p.imprint)                                 AS imprint,
       COALESCE(rou.common_privacy, p.common_privacy)                   AS common_privacy,
       COALESCE(rou.common_accessibility, p.common_accessibility)       AS common_accessibility,
       COALESCE(rou.technical_support_email, p.technical_support_email) AS technical_support_email,
       COALESCE(rou.special_support_email, p.special_support_email)     AS special_support_email,
       rou.created,
       rou.updated,
       COALESCE(rou.theme_id, p.theme_id)                               AS theme_id,
       COALESCE(rou.technical_support_phone, p.technical_support_phone) AS technical_support_phone,
       COALESCE(rou.technical_support_info, p.technical_support_info)   AS technical_support_info,
       COALESCE(rou.special_support_phone, p.special_support_phone)     AS special_support_phone,
       COALESCE(rou.special_support_info, p.special_support_info)       AS special_support_info,
       COALESCE(rou.default_mail_signature, p.default_mail_signature)   AS default_mail_signature,
       rou.depth,
       rou.parent_department_id,
       p.parent_names || p.name                                         AS parent_names,
       p.parent_ids || p.id                                             AS parent_ids
FROM departments rou
         JOIN departments_cte p ON p.id = rou.parent_department_id)
SELECT DISTINCT ON (id) id,
    name,
    postal_address,
    imprint,
    common_privacy,
    common_accessibility,
    technical_support_email,
    special_support_email,
    created,
    updated,
    theme_id,
    technical_support_phone,
    technical_support_info,
    special_support_phone,
    special_support_info,
    default_mail_signature,
    depth,
    parent_department_id,
    parent_names,
    parent_ids
FROM departments_cte
ORDER BY id, depth DESC;

create view v_user_is_recursively_deputy_for(deputy_user_id, original_user_id, depth) as
WITH RECURSIVE deputy_hierarchy AS (SELECT ud.original_user_id,
                                           ud.deputy_user_id,
                                           1 AS depth
                                    FROM user_deputies ud
                                    WHERE ud.from_date <= CURRENT_DATE
                                      AND (ud.until_date IS NULL OR CURRENT_DATE <= ud.until_date)
                                    UNION ALL
                                    SELECT dh_1.original_user_id,
                                           ud.deputy_user_id,
                                           dh_1.depth + 1 AS depth
                                    FROM user_deputies ud
                                             JOIN deputy_hierarchy dh_1
                                                  ON ud.original_user_id::text = dh_1.deputy_user_id::text
        WHERE ud.from_date <= CURRENT_DATE
        AND (ud.until_date IS NULL OR CURRENT_DATE <= ud.until_date))
SELECT DISTINCT deputy_user_id,
                original_user_id,
                depth
FROM deputy_hierarchy dh;

create view v_user_system_permission(user_id, permissions) as
SELECT u.id                                   AS user_id,
       array_unique_union_agg(sr.permissions) AS permissions
FROM (SELECT u_1.id,
             u_1.system_role_id
      FROM users u_1
      UNION ALL
      SELECT v.deputy_user_id AS id,
             u_original.system_role_id
      FROM v_user_is_recursively_deputy_for v
               JOIN users u_original ON v.original_user_id::text = u_original.id::text) u
         JOIN system_roles sr ON u.system_role_id = sr.id
GROUP BY u.id;

create view v_user_deputy_with_details(id, from_date, until_date, active, original_user_id, original_user_email, original_user_first_name, original_user_last_name, original_user_enabled, original_user_verified, original_user_deleted_in_idp, original_user_system_role_id, original_user_full_name, deputy_user_id, deputy_user_email, deputy_user_first_name, deputy_user_last_name, deputy_user_enabled, deputy_user_verified, deputy_user_deleted_in_idp, deputy_user_system_role_id, deputy_user_full_name) as
SELECT udep.id,
       udep.from_date,
       udep.until_date,
       udep.from_date <= CURRENT_DATE AND (udep.until_date IS NULL OR CURRENT_DATE <= udep.until_date) AS active,
       ou.id                                                                                           AS original_user_id,
       ou.email                                                                                        AS original_user_email,
       ou.first_name                                                                                   AS original_user_first_name,
       ou.last_name                                                                                    AS original_user_last_name,
       ou.enabled                                                                                      AS original_user_enabled,
       ou.verified                                                                                     AS original_user_verified,
       ou.deleted_in_idp                                                                               AS original_user_deleted_in_idp,
       ou.system_role_id                                                                               AS original_user_system_role_id,
       ou.full_name                                                                                    AS original_user_full_name,
       du.id                                                                                           AS deputy_user_id,
       du.email                                                                                        AS deputy_user_email,
       du.first_name                                                                                   AS deputy_user_first_name,
       du.last_name                                                                                    AS deputy_user_last_name,
       du.enabled                                                                                      AS deputy_user_enabled,
       du.verified                                                                                     AS deputy_user_verified,
       du.deleted_in_idp                                                                               AS deputy_user_deleted_in_idp,
       du.system_role_id                                                                               AS deputy_user_system_role_id,
       du.full_name                                                                                    AS deputy_user_full_name
FROM user_deputies udep
         JOIN users ou ON udep.original_user_id::text = ou.id::text
         JOIN users du ON udep.deputy_user_id::text = du.id::text;

create view v_department_memberships_with_details(membership_id, membership_has_deputies, membership_deputies, user_id, user_email, user_first_name, user_last_name, user_full_name, user_enabled, user_verified, user_deleted_in_idp, user_system_role_id, department_id, department_name, department_postal_address, department_imprint, department_common_privacy, department_common_accessibility, department_technical_support_email, department_technical_support_phone, department_technical_support_info, department_special_support_email, department_special_support_phone, department_special_support_info, department_theme_id, department_default_mail_signature, department_depth, department_parent_department_id, department_parent_names, department_parent_ids, domain_roles, domain_role_assignments, domain_role_permissions) as
WITH aggregated_domain_roles AS (SELECT dra.department_membership_id,
                                        jsonb_agg(dr.*)                        AS domain_roles,
                                        jsonb_agg(dra.*)                       AS domain_role_assignments,
                                        array_unique_union_agg(dr.permissions) AS domain_role_permissions
                                 FROM domain_role_assignments dra
                                          JOIN domain_roles dr ON dr.id = dra.domain_role_id
                                 GROUP BY dra.department_membership_id),
     aggregated_deputies AS (SELECT dep_1.original_user_id,
                                    jsonb_agg(usr.*) AS deputies
                             FROM v_user_is_recursively_deputy_for dep_1
                                      JOIN users usr ON dep_1.deputy_user_id::text = usr.id::text
        GROUP BY dep_1.original_user_id)
SELECT mem.id                                                           AS membership_id,
       jsonb_array_length(COALESCE(ad.deputies, '[]'::jsonb)) > 0       AS membership_has_deputies,
       COALESCE(ad.deputies, '[]'::jsonb)                               AS membership_deputies,
       original_usr.id                                                  AS user_id,
       original_usr.email                                               AS user_email,
       original_usr.first_name                                          AS user_first_name,
       original_usr.last_name                                           AS user_last_name,
       original_usr.full_name                                           AS user_full_name,
       original_usr.enabled                                             AS user_enabled,
       original_usr.verified                                            AS user_verified,
       original_usr.deleted_in_idp                                      AS user_deleted_in_idp,
       original_usr.system_role_id                                      AS user_system_role_id,
       dep.id                                                           AS department_id,
       dep.name                                                         AS department_name,
       dep.postal_address                                               AS department_postal_address,
       dep.imprint                                                      AS department_imprint,
       dep.common_privacy                                               AS department_common_privacy,
       dep.common_accessibility                                         AS department_common_accessibility,
       dep.technical_support_email                                      AS department_technical_support_email,
       dep.technical_support_phone                                      AS department_technical_support_phone,
       dep.technical_support_info                                       AS department_technical_support_info,
       dep.special_support_email                                        AS department_special_support_email,
       dep.special_support_phone                                        AS department_special_support_phone,
       dep.special_support_info                                         AS department_special_support_info,
       dep.theme_id                                                     AS department_theme_id,
       dep.default_mail_signature                                       AS department_default_mail_signature,
       dep.depth                                                        AS department_depth,
       dep.parent_department_id                                         AS department_parent_department_id,
       dep.parent_names                                                 AS department_parent_names,
       dep.parent_ids                                                   AS department_parent_ids,
       COALESCE(adr.domain_roles, '[]'::jsonb)                          AS domain_roles,
       COALESCE(adr.domain_role_assignments, '[]'::jsonb)               AS domain_role_assignments,
       COALESCE(adr.domain_role_permissions, '{}'::character varying[]) AS domain_role_permissions
FROM department_memberships mem
         LEFT JOIN v_departments_shadowed dep ON dep.id = mem.department_id
         LEFT JOIN users original_usr ON original_usr.id::text = mem.user_id::text
         LEFT JOIN aggregated_deputies ad ON ad.original_user_id::text = mem.user_id::text
    LEFT JOIN aggregated_domain_roles adr ON adr.department_membership_id = mem.id;

create view v_user_department_permissions(user_id, department_id, is_direct_member, is_indirect_member, direct_system_role_permissions, indirect_system_role_permissions, system_role_permissions, system_role_names, system_role_ids, direct_domain_role_permissions, indirect_domain_role_permissions, domain_role_permissions, domain_role_names, domain_role_ids, direct_permissions, indirect_permissions, permissions, deputy_for_user_ids) as
WITH aggregated_system_permissions AS (SELECT usr.id                                        AS user_id,
                                              array_unique_union_agg(own_sr.permissions)    AS direct_system_role_permissions,
                                              array_unique_union_agg(deputy_sr.permissions) AS indirect_system_role_permissions,
                                              array_unique_union_agg(sr.permissions)        AS system_role_permissions,
                                              array_agg(DISTINCT sr.name)                   AS system_role_names,
                                              array_agg(DISTINCT sr.id)                     AS system_role_ids,
                                              array_remove(array_agg(DISTINCT dpty.original_user_id),
                                                           NULL::character varying)         AS deputy_for_user_ids
                                       FROM users usr
                                                LEFT JOIN system_roles own_sr ON own_sr.id = usr.system_role_id
                                                LEFT JOIN v_user_is_recursively_deputy_for dpty
                                                          ON usr.id::text = dpty.deputy_user_id::text
        LEFT JOIN users ou ON dpty.original_user_id::text = ou.id::text
        LEFT JOIN system_roles deputy_sr ON deputy_sr.id = ou.system_role_id
        LEFT JOIN system_roles sr
        ON sr.id = usr.system_role_id OR sr.id = ou.system_role_id
        GROUP BY usr.id),
        aggregated_domain_permissions AS (SELECT usr.id AS                                                                     user_id,
        dm.department_id,
        bool_or(dm.user_id::text = usr.id::text) AS                                   is_direct_member,
        bool_or(dpty.original_user_id IS NOT NULL AND dm.user_id::text =
        dpty.original_user_id::text) AS is_indirect_member,
        array_unique_union_agg(
        CASE
        WHEN dm.user_id::text = usr.id::text THEN dr.permissions
        ELSE '{}'::character varying[]
        END) AS                                                           direct_domain_role_permissions,
        array_unique_union_agg(
        CASE
        WHEN dpty.original_user_id IS NOT NULL AND
        dm.user_id::text = dpty.original_user_id::text
        THEN dr.permissions
        ELSE '{}'::character varying[]
        END) AS                                                           indirect_domain_role_permissions,
        array_unique_union_agg(dr.permissions) AS                                     domain_role_permissions,
        array_agg(DISTINCT dr.name) AS                                                domain_role_names,
        array_agg(DISTINCT dr.id) AS                                                  domain_role_ids,
        array_remove(array_agg(DISTINCT dpty.original_user_id),
        NULL::character varying) AS                                      deputy_for_user_ids
        FROM users usr
        LEFT JOIN v_user_is_recursively_deputy_for dpty
        ON usr.id::text = dpty.deputy_user_id::text
        RIGHT JOIN department_memberships dm
        ON dm.user_id::text = dpty.original_user_id::text OR
        dm.user_id::text = usr.id::text
        LEFT JOIN domain_role_assignments dra ON dra.department_membership_id = dm.id
        LEFT JOIN domain_roles dr ON dr.id = dra.domain_role_id
        GROUP BY usr.id, dm.department_id),
        user_department_scope AS (SELECT aggregated_domain_permissions.user_id,
        aggregated_domain_permissions.department_id
        FROM aggregated_domain_permissions
        UNION
        SELECT asp_1.user_id,
        dep.id AS department_id
        FROM aggregated_system_permissions asp_1
        CROSS JOIN departments dep
        WHERE cardinality(asp_1.system_role_permissions) > 0)
SELECT uds.user_id,
       uds.department_id,
       COALESCE(bool_or(adp.is_direct_member), false)                                                           AS is_direct_member,
       COALESCE(bool_or(adp.is_indirect_member), false)                                                         AS is_indirect_member,
       array_unique_union_agg(asp.direct_system_role_permissions)                                               AS direct_system_role_permissions,
       array_unique_union_agg(asp.indirect_system_role_permissions)                                             AS indirect_system_role_permissions,
       array_unique_union_agg(asp.system_role_permissions)                                                      AS system_role_permissions,
       array_unique_union_agg(asp.system_role_names)                                                            AS system_role_names,
       array_unique_union_agg(asp.system_role_ids)                                                              AS system_role_ids,
       array_unique_union_agg(adp.direct_domain_role_permissions)                                               AS direct_domain_role_permissions,
       array_unique_union_agg(adp.indirect_domain_role_permissions)                                             AS indirect_domain_role_permissions,
       array_unique_union_agg(adp.domain_role_permissions)                                                      AS domain_role_permissions,
       array_remove(array_unique_union_agg(adp.domain_role_names),
                    NULL::character varying)                                                                    AS domain_role_names,
       array_remove(array_unique_union_agg(adp.domain_role_ids),
                    NULL::integer)                                                                              AS domain_role_ids,
       array_unique_union_multi_agg(asp.direct_system_role_permissions,
                                    adp.direct_domain_role_permissions)                                         AS direct_permissions,
       array_unique_union_multi_agg(asp.indirect_system_role_permissions,
                                    adp.indirect_domain_role_permissions)                                       AS indirect_permissions,
       array_unique_union_multi_agg(asp.system_role_permissions,
                                    adp.domain_role_permissions)                                                AS permissions,
       array_unique_union_multi_agg(asp.deputy_for_user_ids,
                                    adp.deputy_for_user_ids)                                                    AS deputy_for_user_ids
FROM user_department_scope uds
         LEFT JOIN aggregated_domain_permissions adp
                   ON adp.user_id::text = uds.user_id::text AND adp.department_id = uds.department_id
         LEFT JOIN aggregated_system_permissions asp ON asp.user_id::text = uds.user_id::text
GROUP BY uds.user_id, uds.department_id;

create view v_team_memberships_with_details(membership_id, membership_has_deputies, membership_deputies, user_id, user_email, user_first_name, user_last_name, user_full_name, user_enabled, user_verified, user_deleted_in_idp, user_system_role_id, team_id, team_name, domain_roles, domain_role_assignments, domain_role_permissions) as
WITH aggregated_roles AS (SELECT dra.team_membership_id,
                                 jsonb_agg(dr.*)                        AS domain_roles,
                                 jsonb_agg(dra.*)                       AS domain_role_assignments,
                                 array_unique_union_agg(dr.permissions) AS domain_role_permissions
                          FROM domain_role_assignments dra
                                   JOIN domain_roles dr ON dr.id = dra.domain_role_id
                          GROUP BY dra.team_membership_id),
     aggregated_deputies AS (SELECT dep.original_user_id,
                                    jsonb_agg(usr.*) AS deputies
                             FROM v_user_is_recursively_deputy_for dep
                                      JOIN users usr ON dep.deputy_user_id::text = usr.id::text
        GROUP BY dep.original_user_id)
SELECT mem.id                                                          AS membership_id,
       jsonb_array_length(COALESCE(ad.deputies, '[]'::jsonb)) > 0      AS membership_has_deputies,
       COALESCE(ad.deputies, '[]'::jsonb)                              AS membership_deputies,
       original_usr.id                                                 AS user_id,
       original_usr.email                                              AS user_email,
       original_usr.first_name                                         AS user_first_name,
       original_usr.last_name                                          AS user_last_name,
       original_usr.full_name                                          AS user_full_name,
       original_usr.enabled                                            AS user_enabled,
       original_usr.verified                                           AS user_verified,
       original_usr.deleted_in_idp                                     AS user_deleted_in_idp,
       original_usr.system_role_id                                     AS user_system_role_id,
       team.id                                                         AS team_id,
       team.name                                                       AS team_name,
       COALESCE(ar.domain_roles, '[]'::jsonb)                          AS domain_roles,
       COALESCE(ar.domain_role_assignments, '[]'::jsonb)               AS domain_role_assignments,
       COALESCE(ar.domain_role_permissions, '{}'::character varying[]) AS domain_role_permissions
FROM team_memberships mem
         LEFT JOIN teams team ON team.id = mem.team_id
         LEFT JOIN users original_usr ON original_usr.id::text = mem.user_id::text
         LEFT JOIN aggregated_deputies ad ON ad.original_user_id::text = mem.user_id::text
    LEFT JOIN aggregated_roles ar ON ar.team_membership_id = mem.id;

create view v_user_team_permissions(user_id, team_id, is_direct_member, is_indirect_member, direct_system_role_permissions, indirect_system_role_permissions, system_role_permissions, system_role_names, system_role_ids, direct_domain_role_permissions, indirect_domain_role_permissions, domain_role_permissions, domain_role_names, domain_role_ids, direct_permissions, indirect_permissions, permissions, deputy_for_user_ids) as
WITH aggregated_system_permissions AS (SELECT usr.id                                        AS user_id,
                                              array_unique_union_agg(own_sr.permissions)    AS direct_system_role_permissions,
                                              array_unique_union_agg(deputy_sr.permissions) AS indirect_system_role_permissions,
                                              array_unique_union_agg(sr.permissions)        AS system_role_permissions,
                                              array_agg(DISTINCT sr.name)                   AS system_role_names,
                                              array_agg(DISTINCT sr.id)                     AS system_role_ids,
                                              array_remove(array_agg(DISTINCT dpty.original_user_id),
                                                           NULL::character varying)         AS deputy_for_user_ids
                                       FROM users usr
                                                LEFT JOIN system_roles own_sr ON own_sr.id = usr.system_role_id
                                                LEFT JOIN v_user_is_recursively_deputy_for dpty
                                                          ON usr.id::text = dpty.deputy_user_id::text
        LEFT JOIN users ou ON dpty.original_user_id::text = ou.id::text
        LEFT JOIN system_roles deputy_sr ON deputy_sr.id = ou.system_role_id
        LEFT JOIN system_roles sr
        ON sr.id = usr.system_role_id OR sr.id = ou.system_role_id
        GROUP BY usr.id),
        aggregated_domain_permissions AS (SELECT usr.id AS                                                                     user_id,
        dm.team_id,
        bool_or(dm.user_id::text = usr.id::text) AS                                   is_direct_member,
        bool_or(dpty.original_user_id IS NOT NULL AND dm.user_id::text =
        dpty.original_user_id::text) AS is_indirect_member,
        array_unique_union_agg(
        CASE
        WHEN dm.user_id::text = usr.id::text THEN dr.permissions
        ELSE '{}'::character varying[]
        END) AS                                                           direct_domain_role_permissions,
        array_unique_union_agg(
        CASE
        WHEN dpty.original_user_id IS NOT NULL AND
        dm.user_id::text = dpty.original_user_id::text
        THEN dr.permissions
        ELSE '{}'::character varying[]
        END) AS                                                           indirect_domain_role_permissions,
        array_unique_union_agg(dr.permissions) AS                                     domain_role_permissions,
        array_agg(DISTINCT dr.name) AS                                                domain_role_names,
        array_agg(DISTINCT dr.id) AS                                                  domain_role_ids,
        array_remove(array_agg(DISTINCT dpty.original_user_id),
        NULL::character varying) AS                                      deputy_for_user_ids
        FROM users usr
        LEFT JOIN v_user_is_recursively_deputy_for dpty
        ON usr.id::text = dpty.deputy_user_id::text
        RIGHT JOIN team_memberships dm
        ON dm.user_id::text = dpty.original_user_id::text OR
        dm.user_id::text = usr.id::text
        LEFT JOIN domain_role_assignments dra ON dra.team_membership_id = dm.id
        LEFT JOIN domain_roles dr ON dr.id = dra.domain_role_id
        GROUP BY usr.id, dm.team_id),
        user_team_scope AS (SELECT aggregated_domain_permissions.user_id,
        aggregated_domain_permissions.team_id
        FROM aggregated_domain_permissions
        UNION
        SELECT asp_1.user_id,
        tms.id AS team_id
        FROM aggregated_system_permissions asp_1
        CROSS JOIN teams tms
        WHERE cardinality(asp_1.system_role_permissions) > 0)
SELECT uts.user_id,
       uts.team_id,
       COALESCE(bool_or(adp.is_direct_member), false)                                                           AS is_direct_member,
       COALESCE(bool_or(adp.is_indirect_member), false)                                                         AS is_indirect_member,
       array_unique_union_agg(asp.direct_system_role_permissions)                                               AS direct_system_role_permissions,
       array_unique_union_agg(asp.indirect_system_role_permissions)                                             AS indirect_system_role_permissions,
       array_unique_union_agg(asp.system_role_permissions)                                                      AS system_role_permissions,
       array_unique_union_agg(asp.system_role_names)                                                            AS system_role_names,
       array_unique_union_agg(asp.system_role_ids)                                                              AS system_role_ids,
       array_unique_union_agg(adp.direct_domain_role_permissions)                                               AS direct_domain_role_permissions,
       array_unique_union_agg(adp.indirect_domain_role_permissions)                                             AS indirect_domain_role_permissions,
       array_unique_union_agg(adp.domain_role_permissions)                                                      AS domain_role_permissions,
       array_remove(array_unique_union_agg(adp.domain_role_names),
                    NULL::character varying)                                                                    AS domain_role_names,
       array_remove(array_unique_union_agg(adp.domain_role_ids),
                    NULL::integer)                                                                              AS domain_role_ids,
       array_unique_union_multi_agg(asp.direct_system_role_permissions,
                                    adp.direct_domain_role_permissions)                                         AS direct_permissions,
       array_unique_union_multi_agg(asp.indirect_system_role_permissions,
                                    adp.indirect_domain_role_permissions)                                       AS indirect_permissions,
       array_unique_union_multi_agg(asp.system_role_permissions,
                                    adp.domain_role_permissions)                                                AS permissions,
       array_unique_union_multi_agg(asp.deputy_for_user_ids,
                                    adp.deputy_for_user_ids)                                                    AS deputy_for_user_ids
FROM user_team_scope uts
         LEFT JOIN aggregated_domain_permissions adp
                   ON adp.user_id::text = uts.user_id::text AND adp.team_id = uts.team_id
         LEFT JOIN aggregated_system_permissions asp ON asp.user_id::text = uts.user_id::text
GROUP BY uts.user_id, uts.team_id;

create view v_user_domain_permissions(user_id, department_id, team_id, is_direct_member, is_indirect_member, direct_system_role_permissions, indirect_system_role_permissions, system_role_permissions, system_role_names, system_role_ids, direct_domain_role_permissions, indirect_domain_role_permissions, domain_role_permissions, domain_role_names, domain_role_ids, direct_permissions, indirect_permissions, permissions, deputy_for_user_ids) as
SELECT dmp.user_id,
       dmp.department_id,
       NULL::integer AS team_id,
    dmp.is_direct_member,
       dmp.is_indirect_member,
       dmp.direct_system_role_permissions,
       dmp.indirect_system_role_permissions,
       dmp.system_role_permissions,
       dmp.system_role_names,
       dmp.system_role_ids,
       dmp.direct_domain_role_permissions,
       dmp.indirect_domain_role_permissions,
       dmp.domain_role_permissions,
       dmp.domain_role_names,
       dmp.domain_role_ids,
       dmp.direct_permissions,
       dmp.indirect_permissions,
       dmp.permissions,
       dmp.deputy_for_user_ids
FROM v_user_department_permissions dmp
UNION
SELECT tmp.user_id,
       NULL::integer AS department_id,
    tmp.team_id,
       tmp.is_direct_member,
       tmp.is_indirect_member,
       tmp.direct_system_role_permissions,
       tmp.indirect_system_role_permissions,
       tmp.system_role_permissions,
       tmp.system_role_names,
       tmp.system_role_ids,
       tmp.direct_domain_role_permissions,
       tmp.indirect_domain_role_permissions,
       tmp.domain_role_permissions,
       tmp.domain_role_names,
       tmp.domain_role_ids,
       tmp.direct_permissions,
       tmp.indirect_permissions,
       tmp.permissions,
       tmp.deputy_for_user_ids
FROM v_user_team_permissions tmp;

create view v_user_process_access_permissions(user_id, via_source_team_id, via_source_department_id, target_process_id, permissions) as
SELECT udp.user_id,
       udp.team_id       AS via_source_team_id,
       udp.department_id AS via_source_department_id,
       pro.id            AS target_process_id,
       CASE
           WHEN pac.permissions IS NULL THEN udp.permissions
           ELSE array_intersect(udp.permissions, pac.permissions)
           END           AS permissions
FROM v_user_domain_permissions udp
         LEFT JOIN process_access_controls pac
                   ON pac.source_department_id = udp.department_id OR pac.source_team_id = udp.team_id
         RIGHT JOIN processes pro ON pac.target_process_id = pro.id OR udp.department_id = pro.department_id;

create view v_user_process_with_details(id, internal_title, department_id, access_key, version_count, drafted_version, published_version, created, updated, department_name, department_postal_address, department_imprint, department_common_privacy, department_common_accessibility, department_technical_support_email, department_special_support_email, department_created, department_updated, department_theme_id, department_technical_support_phone, department_technical_support_info, department_special_support_phone, department_special_support_info, department_default_mail_signature, department_depth, department_parent_department_id, department_parent_names, department_parent_ids) as
SELECT pd.id,
       pd.internal_title,
       pd.department_id,
       pd.access_key,
       pd.version_count,
       pd.drafted_version,
       pd.published_version,
       pd.created,
       pd.updated,
       vds.name                    AS department_name,
       vds.postal_address          AS department_postal_address,
       vds.imprint                 AS department_imprint,
       vds.common_privacy          AS department_common_privacy,
       vds.common_accessibility    AS department_common_accessibility,
       vds.technical_support_email AS department_technical_support_email,
       vds.special_support_email   AS department_special_support_email,
       vds.created                 AS department_created,
       vds.updated                 AS department_updated,
       vds.theme_id                AS department_theme_id,
       vds.technical_support_phone AS department_technical_support_phone,
       vds.technical_support_info  AS department_technical_support_info,
       vds.special_support_phone   AS department_special_support_phone,
       vds.special_support_info    AS department_special_support_info,
       vds.default_mail_signature  AS department_default_mail_signature,
       vds.depth                   AS department_depth,
       vds.parent_department_id    AS department_parent_department_id,
       vds.parent_names            AS department_parent_names,
       vds.parent_ids              AS department_parent_ids
FROM v_user_process_access_permissions updap
         JOIN users u ON u.id::text = updap.user_id::text
         JOIN processes pd ON pd.id = updap.target_process_id
         JOIN v_departments_shadowed vds ON pd.department_id = vds.id
WHERE array_length(updap.permissions, 1) > 0;

create view v_potential_process_instance_access(process_id, process_version, department_id, department_name, team_id, team_name, user_id, user_is_enabled, user_full_name, user_via_department_id, user_via_team_id, user_is_direct_member, user_is_indirect_member, user_is_deputy, user_direct_permissions, user_indirect_permissions, user_as_deputy_for_user_ids, via_process_instance_access_preset_id, permissions) as
SELECT pro.id                      AS process_id,
       ver.process_version,
       dpt.id                      AS department_id,
       dpt.name                    AS department_name,
       NULL::integer               AS team_id,
    NULL::character varying(96) AS team_name,
       NULL::character varying(36) AS user_id,
       NULL::boolean               AS user_is_enabled,
       NULL::text                  AS user_full_name,
       NULL::integer               AS user_via_department_id,
       NULL::integer               AS user_via_team_id,
       false                       AS user_is_direct_member,
       false                       AS user_is_indirect_member,
       false                       AS user_is_deputy,
       NULL::character varying[]   AS user_direct_permissions,
       NULL::character varying[]   AS user_indirect_permissions,
       NULL::character varying[]   AS user_as_deputy_for_user_ids,
       NULL::integer               AS via_process_instance_access_preset_id,
       '{*}'::character varying[]  AS permissions
FROM processes pro
         JOIN process_versions ver ON pro.id = ver.process_id
         JOIN departments dpt ON dpt.id = pro.department_id
UNION
SELECT pro.id                                        AS process_id,
       ver.process_version,
       NULL::integer                                 AS department_id,
    NULL::character varying(96)                   AS department_name,
       NULL::integer                                 AS team_id,
       NULL::character varying(96)                   AS team_name,
       dptp.user_id,
       usr.enabled                                   AS user_is_enabled,
       usr.full_name                                 AS user_full_name,
       dptp.department_id                            AS user_via_department_id,
       NULL::integer                                 AS user_via_team_id,
       dptp.is_direct_member                         AS user_is_direct_member,
       dptp.is_indirect_member                       AS user_is_indirect_member,
       array_length(dptp.deputy_for_user_ids, 1) > 0 AS user_is_deputy,
       dptp.direct_permissions                       AS user_direct_permissions,
       dptp.indirect_permissions                     AS user_indirect_permissions,
       dptp.deputy_for_user_ids                      AS user_as_deputy_for_user_ids,
       NULL::integer                                 AS via_process_instance_access_preset_id,
       dptp.permissions
FROM processes pro
    JOIN process_versions ver ON pro.id = ver.process_id
    JOIN v_user_department_permissions dptp ON dptp.department_id = pro.department_id
    JOIN users usr ON usr.id::text = dptp.user_id::text
UNION
SELECT pro.id                      AS process_id,
       ver.process_version,
       pac.source_department_id    AS department_id,
       dpt.name                    AS department_name,
       pac.source_team_id          AS team_id,
       tm.name                     AS team_name,
       NULL::character varying(36) AS user_id,
       NULL::boolean               AS user_is_enabled,
       NULL::text                  AS user_full_name,
       NULL::integer               AS user_via_department_id,
       NULL::integer               AS user_via_team_id,
       false                       AS user_is_direct_member,
       false                       AS user_is_indirect_member,
       false                       AS user_is_deputy,
       NULL::character varying[]   AS user_direct_permissions,
       NULL::character varying[]   AS user_indirect_permissions,
       NULL::character varying[]   AS user_as_deputy_for_user_ids,
       NULL::integer               AS via_process_instance_access_preset_id,
       pac.permissions
FROM processes pro
    JOIN process_versions ver ON pro.id = ver.process_id
    JOIN process_instance_access_control_presets pac
    ON pac.target_process_id = pro.id AND pac.target_process_version = ver.process_version
    LEFT JOIN departments dpt ON pac.source_department_id = dpt.id
    LEFT JOIN teams tm ON pac.source_team_id = tm.id
UNION
SELECT pro.id                                            AS process_id,
       ver.process_version,
       NULL::integer                                     AS department_id,
    NULL::character varying(96)                       AS department_name,
       NULL::integer                                     AS team_id,
       NULL::character varying(96)                       AS team_name,
       udp.user_id,
       usr.enabled                                       AS user_is_enabled,
       usr.full_name                                     AS user_full_name,
       pac.source_department_id                          AS user_via_department_id,
       pac.source_team_id                                AS user_via_team_id,
       udp.is_direct_member                              AS user_is_direct_member,
       udp.is_indirect_member                            AS user_is_indirect_member,
       array_length(udp.deputy_for_user_ids, 1) > 0      AS user_is_deputy,
       udp.direct_permissions                            AS user_direct_permissions,
       udp.indirect_permissions                          AS user_indirect_permissions,
       udp.deputy_for_user_ids                           AS user_as_deputy_for_user_ids,
       NULL::integer                                     AS via_process_instance_access_preset_id,
       array_intersect(pac.permissions, udp.permissions) AS permissions
FROM processes pro
    JOIN process_versions ver ON pro.id = ver.process_id
    JOIN process_instance_access_control_presets pac
    ON pac.target_process_id = pro.id AND pac.target_process_version = ver.process_version
    JOIN v_user_domain_permissions udp
    ON pac.source_department_id IS NOT NULL AND pac.source_department_id = udp.department_id OR
    pac.source_team_id IS NOT NULL AND pac.source_team_id = udp.team_id
    JOIN users usr ON usr.id::text = udp.user_id::text;

create view v_department_memberships_with_permissions_leg(id, department_id, user_id, created, updated, department_permission_edit, form_permission_create, form_permission_read, form_permission_edit, form_permission_delete, form_permission_annotate, form_permission_publish, process_permission_create, process_permission_read, process_permission_edit, process_permission_delete, process_permission_annotate, process_permission_publish, process_instance_permission_create, process_instance_permission_read, process_instance_permission_edit, process_instance_permission_delete, process_instance_permission_annotate) as
SELECT id,
       department_id,
       user_id,
       created,
       updated,
       false AS department_permission_edit,
       false AS form_permission_create,
       false AS form_permission_read,
       false AS form_permission_edit,
       false AS form_permission_delete,
       false AS form_permission_annotate,
       false AS form_permission_publish,
       false AS process_permission_create,
       false AS process_permission_read,
       false AS process_permission_edit,
       false AS process_permission_delete,
       false AS process_permission_annotate,
       false AS process_permission_publish,
       false AS process_instance_permission_create,
       false AS process_instance_permission_read,
       false AS process_instance_permission_edit,
       false AS process_instance_permission_delete,
       false AS process_instance_permission_annotate
FROM department_memberships dms;

create view v_department_user_role_assignments_with_details(id, department_membership_id, team_membership_id, domain_role_id, created) as
SELECT id,
       department_membership_id,
       team_membership_id,
       domain_role_id,
       created
FROM domain_role_assignments;

create view v_storage_index_items_with_assets(storage_provider_id, path_from_root, directory, filename, mime_type, size_in_bytes, missing, metadata, created, updated, asset_key, asset_uploader_id, asset_is_private) as
SELECT sii.storage_provider_id,
       sii.path_from_root,
       sii.directory,
       sii.filename,
       sii.mime_type,
       sii.size_in_bytes,
       sii.missing,
       sii.metadata,
       sii.created,
       sii.updated,
       ass.key         AS asset_key,
       ass.uploader_id AS asset_uploader_id,
       ass.is_private  AS asset_is_private
FROM storage_index_items sii
         LEFT JOIN assets ass ON sii.storage_provider_id = ass.storage_provider_id AND
                                 sii.path_from_root = ass.storage_path_from_root
         JOIN storage_providers stp ON sii.storage_provider_id = stp.id
WHERE stp.type = 0;

create view v_user_process_instance_access_permissions(user_id, via_source_team_id, via_source_department_id, target_process_instance_id, permissions) as
WITH raw_permissions AS (SELECT udp.user_id,
                                NULL::integer   AS via_source_team_id,
                             p.department_id AS via_source_department_id,
                                pi.id           AS target_process_instance_id,
                                udp.permissions
                         FROM process_instances pi
                                  JOIN processes p ON p.id = pi.process_id
                                  JOIN v_user_domain_permissions udp ON udp.department_id = p.department_id
                         UNION ALL
                         SELECT udp.user_id,
                                piac.source_team_id                                AS via_source_team_id,
                                piac.source_department_id                          AS via_source_department_id,
                                piac.target_process_instance_id,
                                array_intersect(piac.permissions, udp.permissions) AS permissions
                         FROM process_instance_access_controls piac
                                  JOIN v_user_domain_permissions udp ON piac.source_department_id IS NOT NULL AND
                                                                        udp.department_id = piac.source_department_id OR
                                                                        piac.source_team_id IS NOT NULL AND
                                                                        udp.team_id = piac.source_team_id)
SELECT user_id,
       via_source_team_id,
       via_source_department_id,
       target_process_instance_id,
       array_unique_union_agg(permissions) AS permissions
FROM raw_permissions
WHERE array_length(permissions, 1) > 0
GROUP BY user_id, via_source_team_id, via_source_department_id, target_process_instance_id;

create view v_code_list_items(id, code_list_id, columns, created, updated, value, label) as
SELECT cli.id,
       cli.code_list_id,
       cli.columns,
       cli.created,
       cli.updated,
       cli.columns[cl.value_column_index + 1] AS value,
       cli.columns[cl.label_column_index + 1] AS label
FROM code_list_items cli
         JOIN code_lists cl ON cli.code_list_id = cl.id;

create view v_search_items(origin_table, origin_table_subset, label, id, searchable_element, search_text, user_id, permissions) as
SELECT 'assets'::text                                                                     AS origin_table,
    NULL::text                                                                         AS origin_table_subset,
    v_storage_index_items_with_assets.filename                                         AS label,
       (v_storage_index_items_with_assets.storage_provider_id::character varying::text || ','::text) ||
       v_storage_index_items_with_assets.path_from_root::character varying::text          AS id,
       to_tsvector('german'::regconfig, v_storage_index_items_with_assets.filename::text) AS searchable_element,
       v_storage_index_items_with_assets.filename                                         AS search_text,
       usp.user_id,
       usp.permissions
FROM v_storage_index_items_with_assets
         CROSS JOIN v_user_system_permission usp
WHERE v_storage_index_items_with_assets.directory = false
  AND v_storage_index_items_with_assets.missing = false
UNION ALL
SELECT 'data_object_items'::text                                                                    AS origin_table,
    NULL::text                                                                                   AS origin_table_subset,
    data_object_items.id                                                                         AS label,
       (data_object_items.schema_key::text || ','::text) || data_object_items.id::text              AS id,
    to_tsvector('german'::regconfig, data_object_items.id::text) ||
    to_tsvector('german'::regconfig, data_object_items.data)                                     AS searchable_element,
       (data_object_items.id::text || ' '::text) || data_object_items.data::character varying::text AS search_text,
       usp.user_id,
       usp.permissions
FROM data_object_items
    CROSS JOIN v_user_system_permission usp
UNION ALL
SELECT 'data_object_schemas'::text                                      AS origin_table,
    NULL::text                                                       AS origin_table_subset,
    data_object_schemas.name                                         AS label,
       data_object_schemas.key::character varying                       AS id,
       to_tsvector('german'::regconfig, data_object_schemas.name::text) AS searchable_element,
       data_object_schemas.name                                         AS search_text,
       usp.user_id,
       usp.permissions
FROM data_object_schemas
    CROSS JOIN v_user_system_permission usp
UNION ALL
SELECT 'departments'::text                                      AS origin_table,
    NULL::text                                               AS origin_table_subset,
    departments.name                                         AS label,
       departments.id::character varying                        AS id,
       to_tsvector('german'::regconfig, departments.name::text) AS searchable_element,
       departments.name                                         AS search_text,
       udp.user_id,
       udp.permissions
FROM departments
    JOIN v_user_department_permissions udp ON udp.department_id = departments.id
UNION ALL
SELECT 'teams'::text                                      AS origin_table,
    NULL::text                                         AS origin_table_subset,
    teams.name                                         AS label,
       teams.id::character varying                        AS id,
       to_tsvector('german'::regconfig, teams.name::text) AS searchable_element,
       teams.name                                         AS search_text,
       utp.user_id,
       utp.permissions
FROM teams
    JOIN v_user_team_permissions utp ON utp.team_id = teams.id
UNION ALL
SELECT 'users'::text                                                                                                                            AS origin_table,
    NULL::text                                                                                                                               AS origin_table_subset,
    COALESCE(NULLIF(users.full_name, ''::text), users.email::text,
             users.id::text)                                                                                                                 AS label,
       users.id::character varying                                                                                                              AS id,
       to_tsvector('german'::regconfig, COALESCE(users.full_name, ''::text)) || to_tsvector('german'::regconfig,
                                                                                            COALESCE(users.email, ''::character varying)::text) AS searchable_element,
       TRIM(BOTH FROM (COALESCE(users.full_name, ''::text) || ' '::text) ||
                      COALESCE(users.email, ''::character varying)::text)                                                                       AS search_text,
       usp.user_id,
       usp.permissions
FROM users
    CROSS JOIN v_user_system_permission usp
WHERE users.deleted_in_idp = false
UNION ALL
SELECT 'process_nodes'::text                                                           AS origin_table,
    process_nodes.process_node_definition_key::character varying                    AS origin_table_subset,
       COALESCE(process_nodes.name, process_nodes.data_key, ''::character varying)     AS label,
       process_nodes.id::character varying                                             AS id,
       to_tsvector('german'::regconfig, COALESCE(process_nodes.name, ''::character varying)::text) ||
       to_tsvector('german'::regconfig, COALESCE(process_nodes.description, ''::text)) AS searchable_element,
       TRIM(BOTH FROM (COALESCE(process_nodes.name, ''::character varying)::text || ' '::text) ||
                      COALESCE(process_nodes.description, ''::text))                   AS search_text,
       upp.user_id,
       array_unique_union_agg(upp.permissions)                                         AS permissions
FROM process_nodes
    JOIN v_user_process_access_permissions upp ON upp.target_process_id = process_nodes.process_id
WHERE COALESCE(process_nodes.name, ''::character varying)::text <> ''::text
   OR COALESCE(process_nodes.description, ''::text) <> ''::text
GROUP BY process_nodes.id, process_nodes.process_node_definition_key, process_nodes.name, process_nodes.data_key,
    process_nodes.description, upp.user_id
UNION ALL
SELECT 'identity_providers'::text                                      AS origin_table,
    NULL::text                                                      AS origin_table_subset,
    identity_providers.name                                         AS label,
       identity_providers.key::character varying                       AS id,
       to_tsvector('german'::regconfig, identity_providers.name::text) AS searchable_element,
       identity_providers.name                                         AS search_text,
       usp.user_id,
       usp.permissions
FROM identity_providers
    CROSS JOIN v_user_system_permission usp
UNION ALL
SELECT 'payment_providers'::text                                      AS origin_table,
    NULL::text                                                     AS origin_table_subset,
    payment_providers.name                                         AS label,
       payment_providers.key::character varying                       AS id,
       to_tsvector('german'::regconfig, payment_providers.name::text) AS searchable_element,
       payment_providers.name                                         AS search_text,
       usp.user_id,
       usp.permissions
FROM payment_providers
    CROSS JOIN v_user_system_permission usp
UNION ALL
SELECT 'storage_providers'::text                                      AS origin_table,
    NULL::text                                                     AS origin_table_subset,
    storage_providers.name                                         AS label,
       storage_providers.id::character varying                        AS id,
       to_tsvector('german'::regconfig, storage_providers.name::text) AS searchable_element,
       storage_providers.name                                         AS search_text,
       usp.user_id,
       usp.permissions
FROM storage_providers
    CROSS JOIN v_user_system_permission usp
UNION ALL
SELECT 'presets'::text                                                           AS origin_table,
    NULL::text                                                                AS origin_table_subset,
    ((preset_version_with_details.title::text || ' (Version '::text) || preset_version_with_details.version) ||
    ')'::text                                                                 AS label,
    (preset_version_with_details.preset_key || ','::text) ||
    preset_version_with_details.version::character varying::text              AS id,
       to_tsvector('german'::regconfig, preset_version_with_details.title::text) AS searchable_element,
       preset_version_with_details.title                                         AS search_text,
       usp.user_id,
       usp.permissions
FROM preset_version_with_details
    CROSS JOIN v_user_system_permission usp
UNION ALL
SELECT 'secrets'::text                                      AS origin_table,
    NULL::text                                           AS origin_table_subset,
    secrets.name                                         AS label,
       secrets.key::character varying                       AS id,
       to_tsvector('german'::regconfig, secrets.name::text) AS searchable_element,
       secrets.name                                         AS search_text,
       usp.user_id,
       usp.permissions
FROM secrets
    CROSS JOIN v_user_system_permission usp
UNION ALL
SELECT 'themes'::text                                      AS origin_table,
    NULL::text                                          AS origin_table_subset,
    themes.name                                         AS label,
       themes.id::character varying                        AS id,
       to_tsvector('german'::regconfig, themes.name::text) AS searchable_element,
       themes.name                                         AS search_text,
       usp.user_id,
       usp.permissions
FROM themes
    CROSS JOIN v_user_system_permission usp
UNION ALL
SELECT 'code_lists'::text                                                           AS origin_table,
    NULL::text                                                                   AS origin_table_subset,
    code_lists.name                                                              AS label,
       code_lists.key                                                               AS id,
       to_tsvector('german'::regconfig, code_lists.name::text) ||
       to_tsvector('german'::regconfig, code_lists.key::text)                       AS searchable_element,
       TRIM(BOTH FROM (code_lists.name::text || ' '::text) || code_lists.key::text) AS search_text,
       usp.user_id,
       usp.permissions
FROM code_lists
         CROSS JOIN v_user_system_permission usp
UNION ALL
SELECT 'domain_roles'::text                                      AS origin_table,
    NULL::text                                                AS origin_table_subset,
    domain_roles.name                                         AS label,
       domain_roles.id::character varying                        AS id,
       to_tsvector('german'::regconfig, domain_roles.name::text) AS searchable_element,
       domain_roles.name                                         AS search_text,
       usp.user_id,
       usp.permissions
FROM domain_roles
    CROSS JOIN v_user_system_permission usp
UNION ALL
SELECT 'system_roles'::text                                      AS origin_table,
    NULL::text                                                AS origin_table_subset,
    system_roles.name                                         AS label,
       system_roles.id::character varying                        AS id,
       to_tsvector('german'::regconfig, system_roles.name::text) AS searchable_element,
       system_roles.name                                         AS search_text,
       usp.user_id,
       usp.permissions
FROM system_roles
    CROSS JOIN v_user_system_permission usp
UNION ALL
SELECT 'processes'::text                                                                   AS origin_table,
    NULL::text                                                                          AS origin_table_subset,
    ((p.internal_title::text || ' (Version '::text) || pv.process_version) || ')'::text AS label,
    (p.id::character varying::text || ','::text) || pv.process_version                  AS id,
       to_tsvector('german'::regconfig, p.internal_title::text)                            AS searchable_element,
       p.internal_title                                                                    AS search_text,
       usp.user_id,
       array_unique_union_agg(usp.permissions)                                             AS permissions
FROM process_versions pv
         JOIN processes p ON pv.process_id = p.id
         JOIN v_user_process_access_permissions usp ON usp.target_process_id = p.id
GROUP BY p.id, p.internal_title, pv.process_version, usp.user_id
UNION ALL
SELECT 'process_instances'::text                                                               AS origin_table,
    NULL::text                                                                              AS origin_table_subset,
    pi.case_number::character varying::text ||
       CASE
           WHEN pi.assigned_file_numbers = '{}'::character varying[] THEN ''::text
           ELSE (' ('::text || array_to_string(pi.assigned_file_numbers, ', '::text)) || ')'::text
END                                                                                 AS label,
       pi.id::character varying                                                                AS id,
       to_tsvector('german'::regconfig, pi.case_number::character varying::text) ||
       to_tsvector('german'::regconfig, array_to_string(pi.assigned_file_numbers, ', '::text)) AS searchable_element,
       concat_ws(' '::text, pi.case_number, array_to_string(pi.assigned_file_numbers, ', '::text),
                 compact_case_number_search_key(pi.case_number::text))                         AS search_text,
       upp.user_id,
       array_unique_union_agg(upp.permissions)                                                 AS permissions
FROM process_instances pi
         JOIN v_user_process_instance_access_permissions upp ON upp.target_process_instance_id = pi.id
GROUP BY pi.id, pi.case_number, pi.assigned_file_numbers, upp.user_id;

