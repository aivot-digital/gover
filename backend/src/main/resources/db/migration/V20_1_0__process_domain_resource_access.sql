-- create a view to get a users permissions for a process definition based on their team/department memberships
create view v_user_process_access_permissions as
select udp.user_id                                                      as user_id,
       udp.team_id                                                      as via_source_team_id,
       udp.department_id                                                as via_source_department_id,
       pro.id                                                           as target_process_id,
       (case
            when (pac.permissions is null) then udp.permissions
            else array_intersect(udp.permissions, pac.permissions) end) as permissions
from v_user_domain_permissions as udp
         left join process_access_controls pac
                   on pac.source_department_id = udp.department_id or
                      pac.source_team_id = udp.team_id
         right join processes as pro
                   on pac.target_process_id = pro.id or
                      udp.department_id = pro.department_id;

-- create a view to get the full process definition a user has access to
create view v_user_process_with_details as
select pd.*,

       vds.name                      as department_name,
       vds.postal_address            as department_postal_address,
       vds.imprint                   as department_imprint,
       vds.common_privacy            as department_common_privacy,
       vds.common_accessibility      as department_common_accessibility,
       vds.technical_support_email   as department_technical_support_email,
       vds.special_support_email     as department_special_support_email,
       vds.created                   as department_created,
       vds.updated                   as department_updated,
       vds.theme_id                  as department_theme_id,
       vds.technical_support_phone   as department_technical_support_phone,
       vds.technical_support_info    as department_technical_support_info,
       vds.special_support_phone     as department_special_support_phone,
       vds.special_support_info      as department_special_support_info,
       vds.default_mail_signature    as department_default_mail_signature,
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
