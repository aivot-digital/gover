drop view v_user_process_instance_access_permissions;

create view v_user_process_instance_access_permissions as
select udp.user_id                                                    as user_id,
       pac.source_team_id                                             as via_source_team_id,
       case
           when pac is null then udp.department_id
           else pac.source_department_id end                          as via_source_department_id,
       pi.id                                                          as target_process_instance_id,
       case
           when pac is null then udp.permissions
           else array_intersect(pac.permissions, udp.permissions) end as permissions
from v_user_domain_permissions udp
         left join processes pro
                   on pro.department_id = udp.department_id
         join process_instances pi
              on pi.process_id = pro.id
         left join process_access_controls pac
                   on pac.source_department_id = udp.department_id or
                      pac.source_team_id = udp.team_id;