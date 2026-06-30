-- create example team memberships
insert into team_memberships (id, team_id, user_id)
values (1, 1, '00000000-0000-0000-0000-000000000001'),
       (2, 1, '00000000-0000-0000-0000-000000000004'),
       (3, 1, '00000000-0000-0000-0000-000000000008'),
       (4, 1, '00000000-0000-0000-0000-000000000012'),
       (5, 1, '00000000-0000-0000-0000-000000000016'),

       (6, 2, '00000000-0000-0000-0000-000000000002'),
       (7, 2, '00000000-0000-0000-0000-000000000005'),
       (8, 2, '00000000-0000-0000-0000-000000000009'),
       (9, 2, '00000000-0000-0000-0000-000000000013'),
       (10, 2, '00000000-0000-0000-0000-000000000017')
on conflict (id) do update
    set team_id = excluded.team_id,
        user_id = excluded.user_id,
        updated = now();

-- assign roles to the memberships
insert into domain_role_assignments (team_membership_id,
                                     domain_role_id)
select mem.id                                                                      as team_membership_id,
       (select id
        from domain_roles
        offset length(u.full_name) % (select count(id) from domain_roles) limit 1) as domain_role_id
from team_memberships mem
         join users u
              on u.id = mem.user_id
on conflict do nothing;

-- fix id sequence for team_memberships
select setval('team_memberships_id_seq',
              (select max(id) from team_memberships));
