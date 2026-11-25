-- Optimistic locking for role changes of department and team members: the role dialogs save the role set of a
-- membership in one request, which checks and raises the lock version of the membership row.
alter table department_memberships
    add column if not exists lock_version bigint not null default 0;

alter table team_memberships
    add column if not exists lock_version bigint not null default 0;

-- The edit dialogs take the lock version from these list views, so they expose it as their last column.

create or replace view v_user_deputy_with_details(id, from_date, until_date, active, original_user_id, original_user_email, original_user_first_name, original_user_last_name, original_user_enabled, original_user_verified, original_user_deleted_in_idp, original_user_system_role_id, original_user_full_name, deputy_user_id, deputy_user_email, deputy_user_first_name, deputy_user_last_name, deputy_user_enabled, deputy_user_verified, deputy_user_deleted_in_idp, deputy_user_system_role_id, deputy_user_full_name, lock_version) as
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
       du.full_name                                                                                    AS deputy_user_full_name,
       udep.lock_version
FROM user_deputies udep
         JOIN users ou ON udep.original_user_id::text = ou.id::text
         JOIN users du ON udep.deputy_user_id::text = du.id::text;

create or replace view v_department_memberships_with_details(membership_id, membership_has_deputies, membership_deputies, user_id, user_email, user_first_name, user_last_name, user_full_name, user_enabled, user_verified, user_deleted_in_idp, user_system_role_id, department_id, department_name, department_postal_address, department_imprint, department_common_privacy, department_common_accessibility, department_technical_support_email, department_technical_support_phone, department_technical_support_info, department_special_support_email, department_special_support_phone, department_special_support_info, department_theme_id, department_default_mail_signature, department_depth, department_parent_department_id, department_parent_names, department_parent_ids, domain_roles, domain_role_assignments, domain_role_permissions, lock_version) as
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
       COALESCE(adr.domain_role_permissions, '{}'::character varying[]) AS domain_role_permissions,
       mem.lock_version
FROM department_memberships mem
         LEFT JOIN v_departments_shadowed dep ON dep.id = mem.department_id
         LEFT JOIN users original_usr ON original_usr.id::text = mem.user_id::text
         LEFT JOIN aggregated_deputies ad ON ad.original_user_id::text = mem.user_id::text
    LEFT JOIN aggregated_domain_roles adr ON adr.department_membership_id = mem.id;

create or replace view v_team_memberships_with_details(membership_id, membership_has_deputies, membership_deputies, user_id, user_email, user_first_name, user_last_name, user_full_name, user_enabled, user_verified, user_deleted_in_idp, user_system_role_id, team_id, team_name, domain_roles, domain_role_assignments, domain_role_permissions, lock_version) as
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
       COALESCE(ar.domain_role_permissions, '{}'::character varying[]) AS domain_role_permissions,
       mem.lock_version
FROM team_memberships mem
         LEFT JOIN teams team ON team.id = mem.team_id
         LEFT JOIN users original_usr ON original_usr.id::text = mem.user_id::text
         LEFT JOIN aggregated_deputies ad ON ad.original_user_id::text = mem.user_id::text
    LEFT JOIN aggregated_roles ar ON ar.team_membership_id = mem.id;
