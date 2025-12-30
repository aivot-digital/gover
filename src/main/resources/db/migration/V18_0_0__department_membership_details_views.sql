-- create a view to show deputy department memberships with details from
-- 1. the user
-- 2. the department
-- 3. the domain roles

create view v_department_memberships_with_details as
with aggregated_roles as (select dra.department_membership_id           as department_membership_id,
                                 jsonb_agg(dr)                          as domain_roles,
                                 array_unique_union_agg(dr.permissions) as domain_role_permissions
                          from domain_role_assignments dra
                                   join domain_roles dr on dr.id = dra.domain_role_id
                          group by dra.department_membership_id),
     aggregated_deputies as (select dep.original_user_id as original_user_id,
                                    jsonb_agg(usr)       as deputies
                             from v_user_is_recursively_deputy_for dep
                                      join users usr on dep.deputy_user_id = usr.id
                             group by dep.original_user_id)
select mem.id                                              as membership_id,
       jsonb_array_length(coalesce(ad.deputies, '[]')) > 0 as membership_has_deputies,
       coalesce(ad.deputies, '[]')                         as membership_deputies,

       original_usr.id                                     as user_id,
       original_usr.email                                  as user_email,
       original_usr.first_name                             as user_first_name,
       original_usr.last_name                              as user_last_name,
       original_usr.full_name                              as user_full_name,
       original_usr.enabled                                as user_enabled,
       original_usr.verified                               as user_verified,
       original_usr.deleted_in_idp                         as user_deleted_in_idp,
       original_usr.system_role_id                         as user_system_role_id,

       dep.id                                              as department_id,
       dep.name                                            as department_name,
       dep.address                                         as department_address,
       dep.imprint                                         as department_imprint,
       dep.common_privacy                                  as department_common_privacy,
       dep.common_accessibility                            as department_common_accessibility,
       dep.technical_support_address                       as department_technical_support_address,
       dep.technical_support_phone                         as department_technical_support_phone,
       dep.technical_support_info                          as department_technical_support_info,
       dep.special_support_address                         as department_special_support_address,
       dep.special_support_phone                           as department_special_support_phone,
       dep.special_support_info                            as department_special_support_info,
       dep.department_mail                                 as department_mail,
       dep.theme_id                                        as department_theme_id,
       dep.additional_info                                 as department_additional_info,
       dep.depth                                           as department_depth,
       dep.parent_department_id                            as department_parent_department_id,
       dep.parent_names                                    as department_parent_names,
       dep.parent_ids                                      as department_parent_ids,

       coalesce(ar.domain_roles, '[]')                     as domain_roles,
       coalesce(ar.domain_role_permissions, '{}')          as domain_role_permissions
from department_memberships mem
         left join v_departments_shadowed dep on dep.id = mem.department_id
         left join users original_usr on original_usr.id = mem.user_id
         left join aggregated_deputies as ad on ad.original_user_id = mem.user_id
         left join aggregated_roles ar on ar.department_membership_id = mem.id;
