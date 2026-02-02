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
