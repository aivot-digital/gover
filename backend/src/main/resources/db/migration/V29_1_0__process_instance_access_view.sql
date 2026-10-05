drop view v_user_process_instance_access_permissions;

create view v_user_process_instance_access_permissions as
with raw_permissions as (
    -- implicit access through the owning process department
    select udp.user_id,
           null::integer   as via_source_team_id,
           p.department_id as via_source_department_id,
           pi.id           as target_process_instance_id,
           udp.permissions as permissions
    from process_instances pi
             join processes p
                  on p.id = pi.process_id
             join v_user_domain_permissions udp
                  on udp.department_id = p.department_id

    union all

    -- explicit runtime process-instance ACLs
    select udp.user_id,
           piac.source_team_id                                as via_source_team_id,
           piac.source_department_id                          as via_source_department_id,
           piac.target_process_instance_id                    as target_process_instance_id,
           array_intersect(piac.permissions, udp.permissions) as permissions
    from process_instance_access_controls piac
             join v_user_domain_permissions udp
                  on (piac.source_department_id is not null and udp.department_id = piac.source_department_id)
                      or (piac.source_team_id is not null and udp.team_id = piac.source_team_id))
select user_id,
       via_source_team_id,
       via_source_department_id,
       target_process_instance_id,
       array_unique_union_agg(permissions) as permissions
from raw_permissions
where array_length(permissions, 1) > 0
group by user_id, via_source_team_id, via_source_department_id, target_process_instance_id;
