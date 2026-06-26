-- create a view that shows department memberships with user details, department details and details about the assigned domain roles.
-- also include the active deputies for the user in the department membership.
create view v_department_memberships_with_details as
with aggregated_domain_roles as (select dra.department_membership_id           as department_membership_id,
                                        jsonb_agg(dr)                          as domain_roles,
                                        jsonb_agg(dra)                         as domain_role_assignments,
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

       coalesce(adr.domain_roles, '[]')                    as domain_roles,
       coalesce(adr.domain_role_assignments, '[]')         as domain_role_assignments,
       coalesce(adr.domain_role_permissions, '{}')         as domain_role_permissions
from department_memberships mem
         left join v_departments_shadowed dep on dep.id = mem.department_id
         left join users original_usr on original_usr.id = mem.user_id
         left join aggregated_deputies as ad on ad.original_user_id = mem.user_id
         left join aggregated_domain_roles adr on adr.department_membership_id = mem.id;

-- create a view, which shows all permissions a user has in a specific department, based on their own department memberships as well as the memberships of users the user is currently a deputy.
-- the permissions include the domain role permissions assigned to the department memberships as well as the system role permissions of the users and all users the user is deputy for.
create view v_user_department_permissions as
with aggregated_system_permissions as (select usr.id                                                        as user_id,
                                              array_unique_union_agg(own_sr.permissions)                    as direct_system_role_permissions,
                                              array_unique_union_agg(deputy_sr.permissions)                 as indirect_system_role_permissions,
                                              array_unique_union_agg(sr.permissions)                        as system_role_permissions,
                                              array_agg(distinct sr.name)                                   as system_role_names,
                                              array_agg(distinct sr.id)                                     as system_role_ids,
                                              array_remove(array_agg(distinct dpty.original_user_id), null) as deputy_for_user_ids
                                       from users usr
                                                left join system_roles own_sr
                                                          on own_sr.id = usr.system_role_id
                                                left join v_user_is_recursively_deputy_for dpty
                                                          on usr.id = dpty.deputy_user_id
                                                left join users ou
                                                          on dpty.original_user_id = ou.id
                                                left join system_roles deputy_sr
                                                          on deputy_sr.id = ou.system_role_id
                                                left join system_roles sr
                                                          on sr.id = usr.system_role_id or
                                                             sr.id = ou.system_role_id
                                       group by usr.id),
     aggregated_domain_permissions as (select usr.id                                                        as user_id,
                                              dm.department_id                                              as department_id,
                                              bool_or(dm.user_id = usr.id)                                  as is_direct_member,
                                              bool_or(dpty.original_user_id is not null and
                                                      dm.user_id =
                                                      dpty.original_user_id)                                as is_indirect_member,
                                              array_unique_union_agg(
                                                      case
                                                          when dm.user_id = usr.id
                                                              then dr.permissions
                                                          else '{}'::varchar[]
                                                          end
                                              )                                                             as direct_domain_role_permissions,
                                              array_unique_union_agg(
                                                      case
                                                          when dpty.original_user_id is not null and
                                                               dm.user_id = dpty.original_user_id
                                                              then dr.permissions
                                                          else '{}'::varchar[]
                                                          end
                                              )                                                             as indirect_domain_role_permissions,
                                              array_unique_union_agg(dr.permissions)                        as domain_role_permissions,
                                              array_agg(distinct dr.name)                                   as domain_role_names,
                                              array_agg(distinct dr.id)                                     as domain_role_ids,
                                              array_remove(array_agg(distinct dpty.original_user_id), null) as deputy_for_user_ids
                                       from users usr
                                                left join v_user_is_recursively_deputy_for dpty
                                                          on usr.id = dpty.deputy_user_id
                                                left join department_memberships dm
                                                          on dm.user_id = dpty.original_user_id or
                                                             dm.user_id = usr.id
                                                left join domain_role_assignments dra
                                                          on dra.department_membership_id = dm.id
                                                left join domain_roles dr
                                                          on dr.id = dra.domain_role_id
                                       group by usr.id, dm.department_id)
select usr.id                                                                                 as user_id,
       adp.department_id                                                                      as department_id,
       bool_or(adp.is_direct_member)                                                          as is_direct_member,
       bool_or(adp.is_indirect_member)                                                        as is_indirect_member,
       array_unique_union_agg(asp.direct_system_role_permissions)                             as direct_system_role_permissions,
       array_unique_union_agg(asp.indirect_system_role_permissions)                           as indirect_system_role_permissions,
       array_unique_union_agg(asp.system_role_permissions)                                    as system_role_permissions,
       array_unique_union_agg(asp.system_role_names)                                          as system_role_names,
       array_unique_union_agg(asp.system_role_ids)                                            as system_role_ids,
       array_unique_union_agg(adp.direct_domain_role_permissions)                             as direct_domain_role_permissions,
       array_unique_union_agg(adp.indirect_domain_role_permissions)                           as indirect_domain_role_permissions,
       array_unique_union_agg(adp.domain_role_permissions)                                    as domain_role_permissions,
       array_remove(array_unique_union_agg(adp.domain_role_names), null)                      as domain_role_names,
       array_remove(array_unique_union_agg(adp.domain_role_ids), null)                        as domain_role_ids,
       array_unique_union_multi_agg(
               asp.direct_system_role_permissions,
               adp.direct_domain_role_permissions
       )                                                                                      as direct_permissions,
       array_unique_union_multi_agg(
               asp.indirect_system_role_permissions,
               adp.indirect_domain_role_permissions
       )                                                                                      as indirect_permissions,
       array_unique_union_multi_agg(asp.system_role_permissions, adp.domain_role_permissions) as permissions,
       array_unique_union_multi_agg(asp.deputy_for_user_ids, adp.deputy_for_user_ids)         as deputy_for_user_ids
from users usr
         left join aggregated_system_permissions asp on asp.user_id = usr.id
         left join aggregated_domain_permissions adp on adp.user_id = usr.id
group by usr.id, adp.department_id;
