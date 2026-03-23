-- create a view to get a users permissions for a process definition based on their team/department memberships
create view v_user_process_instance_access_permissions as
select udp.user_id                                       as user_id,
       pac.source_team_id                                as via_source_team_id,
       pac.source_department_id                          as via_source_department_id,
       pac.target_process_id                             as target_process_id,
       array_intersect(pac.permissions, udp.permissions) as permissions
from v_user_domain_permissions udp
         join process_access_controls pac
              on pac.source_department_id = udp.department_id or
                 pac.source_team_id = udp.team_id;

-- create a view of all users, departments and teams, which have potential access to a process instance, and their permissions
create view v_potential_process_instance_access as

-- The access of the owning departments to the process instances
select pro.id              as process_id,
       ver.process_version as process_version,
       dpt.id              as department_id,
       dpt.name            as department_name,
       null::integer       as team_id,
       null::varchar(96)   as team_name,
       null::varchar(36)   as user_id,
       null::bool          as user_is_enabled,
       null::text          as user_full_name,
       null::integer       as user_via_department_id,
       null::integer       as user_via_team_id,
       false               as user_is_deputy,
       null::varchar[]     as user_as_deputy_for_user_ids,
       null::integer       as via_process_instance_access_preset_id,
       '{"*"}'             as permissions
from processes pro
         join process_versions ver
              on pro.id = ver.process_id
         join departments dpt
              on dpt.id = pro.department_id

union

-- The access of individual users of the owning department to the process instances via their department membership permissions
select pro.id                                        as process_id,
       ver.process_version                           as process_version,
       null::integer                                 as department_id,
       null::varchar(96)                             as department_name,
       null::integer                                 as team_id,
       null::varchar(96)                             as team_name,
       dptp.user_id                                  as user_id,
       usr.enabled                                   as user_is_enabled,
       usr.full_name                                 as user_full_name,
       dptp.department_id                            as user_via_department_id,
       null::integer                                 as user_via_team_id,
       array_length(dptp.deputy_for_user_ids, 1) > 0 as user_is_deputy,
       dptp.deputy_for_user_ids                      as user_as_deputy_for_user_ids,
       null::integer                                 as via_process_instance_access_preset_id,
       dptp.permissions                              as permissions
from processes pro
         join process_versions ver
              on pro.id = ver.process_id
         join v_user_department_permissions dptp
              on dptp.department_id = pro.department_id
         join users usr
              on usr.id = dptp.user_id

union

-- The access of departments and teams to the process instances via access control presets
select pro.id                   as process_id,
       ver.process_version      as process_version,
       pac.source_department_id as department_id,
       dpt.name                 as department_name,
       pac.source_team_id       as team_id,
       tm.name                  as team_name,
       null::varchar(36)        as user_id,
       null::bool               as user_is_enabled,
       null::text               as user_full_name,
       null::integer            as user_via_department_id,
       null::integer            as user_via_team_id,
       false                    as user_is_deputy,
       null::varchar[]          as user_as_deputy_for_user_ids,
       null::integer            as via_process_instance_access_preset_id,
       pac.permissions          as permissions
from processes pro
         join process_versions ver
              on pro.id = ver.process_id
         join process_instance_access_control_presets pac
              on pac.target_process_id = pro.id
         left join departments dpt
                   on pac.source_department_id = dpt.id
         left join teams tm
                   on pac.source_team_id = tm.id

union

-- The access of individual users of teams or departments to the process instances via access control presets and their department or team membership permissions
select pro.id                                            as process_id,
       ver.process_version                               as process_version,
       nulL::integer                                     as department_id,
       nulL::varchar(96)                                 as department_name,
       nulL::integer                                     as team_id,
       nulL::varchar(96)                                 as team_name,
       udp.user_id                                       as user_id,
       usr.enabled                                       as user_is_enabled,
       usr.full_name                                     as user_full_name,
       pac.source_department_id                          as user_via_department_id,
       pac.source_team_id                                as user_via_team_id,
       array_length(udp.deputy_for_user_ids, 1) > 0      as user_is_deputy,
       udp.deputy_for_user_ids                           as user_as_deputy_for_user_ids,
       null::integer                                     as via_process_instance_access_preset_id,
       array_intersect(pac.permissions, udp.permissions) as permissions
from processes pro
         join process_versions ver
              on pro.id = ver.process_id
         join process_instance_access_control_presets pac
              on pac.target_process_id = pro.id
         join v_user_domain_permissions udp
              on (pac.source_department_id is not null and pac.source_department_id = udp.department_id)
                  or (pac.source_team_id is not null and pac.source_team_id = udp.team_id)
         join users usr
              on usr.id = udp.user_id
