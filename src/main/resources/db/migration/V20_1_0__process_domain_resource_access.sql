-- create a view to get a users permissions for a process definition based on their team/department memberships
create view v_user_process_access_permissions as
select udp.user_id                                       as user_id,
       pac.source_team_id                                as via_source_team_id,
       pac.source_department_id                          as via_source_department_id,
       pac.target_process_id                             as target_process_id,
       array_intersect(pac.permissions, udp.permissions) as permissions
from v_user_domain_permissions udp
         join process_access_controls pac
              on pac.source_department_id = udp.department_id or
                 pac.source_team_id = udp.team_id;

-- create a view to get the full process definition a user has access to
create view v_user_process_with_details as
select pd.*,

       u.id                          as user_id,
       u.email                       as user_email,
       u.first_name                  as user_first_name,
       u.last_name                   as user_last_name,
       u.enabled                     as user_enabled,
       u.verified                    as user_verified,
       u.deleted_in_idp              as user_deleted_in_idp,
       u.system_role_id              as user_system_role_id,
       u.full_name                   as user_full_name,

       vds.name                      as department_name,
       vds.address                   as department_address,
       vds.imprint                   as department_imprint,
       vds.common_privacy            as department_common_privacy,
       vds.common_accessibility      as department_common_accessibility,
       vds.technical_support_address as department_technical_support_address,
       vds.special_support_address   as department_special_support_address,
       vds.created                   as department_created,
       vds.updated                   as department_updated,
       vds.department_mail           as department_department_mail,
       vds.theme_id                  as department_theme_id,
       vds.technical_support_phone   as department_technical_support_phone,
       vds.technical_support_info    as department_technical_support_info,
       vds.special_support_phone     as department_special_support_phone,
       vds.special_support_info      as department_special_support_info,
       vds.additional_info           as department_additional_info,
       vds.depth                     as department_depth,
       vds.parent_department_id      as department_parent_department_id,
       vds.parent_names              as department_parent_names,
       vds.parent_ids                as department_parent_ids
from v_user_process_access_permissions updap
         join users u
              on u.id = updap.user_id
         join processes pd
              on pd.id = updap.target_process_id
         join v_departments_shadowed vds
              on pd.department_id = vds.id
where array_length(updap.permissions, 1) > 0;