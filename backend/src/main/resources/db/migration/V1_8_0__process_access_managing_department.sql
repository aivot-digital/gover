-- Access rules only add access for their source. A department's rights on the processes it manages come from its roles
-- alone, so a rule that targets another process must no longer restrict them. This matches
-- v_user_process_instance_access_permissions.
create or replace view v_user_process_access_permissions(user_id, via_source_team_id, via_source_department_id, target_process_id, permissions) as
WITH raw_permissions AS (SELECT udp.user_id,
                                NULL::integer     AS via_source_team_id,
                                pro.department_id AS via_source_department_id,
                                pro.id            AS target_process_id,
                                udp.permissions
                         FROM processes pro
                                  JOIN v_user_domain_permissions udp ON udp.department_id = pro.department_id
                         UNION ALL
                         SELECT udp.user_id,
                                pac.source_team_id                                AS via_source_team_id,
                                pac.source_department_id                          AS via_source_department_id,
                                pac.target_process_id,
                                array_intersect(udp.permissions, pac.permissions) AS permissions
                         FROM process_access_controls pac
                                  JOIN v_user_domain_permissions udp ON pac.source_department_id IS NOT NULL AND
                                                                        udp.department_id = pac.source_department_id OR
                                                                        pac.source_team_id IS NOT NULL AND
                                                                        udp.team_id = pac.source_team_id)
SELECT user_id,
       via_source_team_id,
       via_source_department_id,
       target_process_id,
       array_unique_union_agg(permissions) AS permissions
FROM raw_permissions
WHERE array_length(permissions, 1) > 0
GROUP BY user_id, via_source_team_id, via_source_department_id, target_process_id;
