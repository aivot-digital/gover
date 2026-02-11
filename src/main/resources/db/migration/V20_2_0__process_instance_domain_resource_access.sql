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
       null::integer       as team_id,
       null::varchar(36)   as user_id,
       null::integer       as user_via_department_id,
       null::integer       as user_via_team_id,
       null::integer       as via_process_instance_access_preset_id,
       '{"*"}'             as permissions
from processes pro
         join process_versions ver
              on pro.id = ver.process_id
         join departments dpt
              on dpt.id = pro.department_id

union

-- The access of individual users of the owning department to the process instances via their department membership permissions
select pro.id              as process_id,
       ver.process_version as process_version,
       null::integer       as department_id,
       null::integer       as team_id,
       dptp.user_id        as user_id,
       dptp.department_id  as user_via_department_id,
       null::integer       as user_via_team_id,
       null::integer       as via_process_instance_access_preset_id,
       dptp.permissions    as permissions
from processes pro
         join process_versions ver
              on pro.id = ver.process_id
         join v_user_department_permissions dptp
              on dptp.department_id = pro.department_id

union

-- The access of departments and teams to the process instances via access control presets
select pro.id                   as process_id,
       ver.process_version      as process_version,
       pac.source_department_id as department_id,
       pac.source_team_id       as team_id,
       null::varchar(36)        as user_id,
       null::integer            as user_via_department_id,
       null::integer            as user_via_team_id,
       null::integer            as via_process_instance_access_preset_id,
       pac.permissions          as permissions
from processes pro
         join process_versions ver
              on pro.id = ver.process_id
         join process_instance_access_control_presets pac
              on pac.target_process_id = pro.id

union

-- The access of individual users of teams or departments to the process instances via access control presets and their department or team membership permissions
select pro.id                                            as process_id,
       ver.process_version                               as process_version,
       nulL::integer                                     as department_id,
       nulL::integer                                     as team_id,
       udp.user_id                                       as user_id,
       pac.source_department_id                          as user_via_department_id,
       pac.source_team_id                                as user_via_team_id,
       null::integer                                     as via_process_instance_access_preset_id,
       array_intersect(pac.permissions, udp.permissions) as permissions
from processes pro
         join process_versions ver
              on pro.id = ver.process_id
         join process_instance_access_control_presets pac
              on pac.target_process_id = pro.id
         join v_user_domain_permissions udp
              on (pac.source_department_id is not null and pac.source_department_id = udp.department_id)
                  or (pac.source_team_id is not null and pac.source_team_id = udp.team_id);
