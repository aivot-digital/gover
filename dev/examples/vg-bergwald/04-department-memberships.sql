-- create example department memberships
insert into department_memberships (id,
                                    department_id,
                                    user_id)
select v.column1 as id,
       v.column3 as department_id,
       v.column2 as user_id
from (values (1, '00000000-0000-0000-0000-000000000001', 1),
             (2, '00000000-0000-0000-0000-000000000002', 2),
             (3, '00000000-0000-0000-0000-000000000003', 3),
             (4, '00000000-0000-0000-0000-000000000004', 4),
             (5, '00000000-0000-0000-0000-000000000005', 5),
             (6, '00000000-0000-0000-0000-000000000006', 6),
             (7, '00000000-0000-0000-0000-000000000007', 7),
             (8, '00000000-0000-0000-0000-000000000008', 8),
             (9, '00000000-0000-0000-0000-000000000009', 9),
             (10, '00000000-0000-0000-0000-000000000010', 10),
             (11, '00000000-0000-0000-0000-000000000011', 1),
             (12, '00000000-0000-0000-0000-000000000012', 2),
             (13, '00000000-0000-0000-0000-000000000013', 3),
             (14, '00000000-0000-0000-0000-000000000014', 4),
             (15, '00000000-0000-0000-0000-000000000015', 5),
             (16, '00000000-0000-0000-0000-000000000016', 6),
             (17, '00000000-0000-0000-0000-000000000017', 7),
             (18, '00000000-0000-0000-0000-000000000018', 8),
             (19, '00000000-0000-0000-0000-000000000019', 9),
             (20, '00000000-0000-0000-0000-000000000020', 10)) as v
on conflict (id) do update
    set department_id = excluded.department_id,
        user_id       = excluded.user_id,
        updated       = now();

-- assign roles to the memberships
insert into domain_role_assignments (department_membership_id,
                                     domain_role_id)
select mem.id                                                                      as department_membership_id,
       (select id
        from domain_roles
        offset length(u.full_name) % (select count(id) from domain_roles) limit 1) as domain_role_id
from department_memberships mem
         join users u
              on u.id = mem.user_id
on conflict do nothing;

-- fix id sequence for department_memberships
select setval('department_memberships_id_seq',
              (select max(id) from department_memberships));
