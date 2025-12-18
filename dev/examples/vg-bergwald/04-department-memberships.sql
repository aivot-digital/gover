-- create memberships
insert into department_memberships (department_id,
                                    user_id)
select d.id as department_id,
       u.id as user_id
from users u
         join departments d
              on d.special_support_address ilike '%' || split_part(u.email, '@', 2)
where d.parent_department_id is null
on conflict (department_id, user_id) do nothing;

-- assign roles to the memberships
insert into domain_role_assignments (department_membership_id,
                                     domain_role_id)
select mem.id                                                                                                  as department_membership_id,
       (select id
        from domain_roles
        offset length(u.full_name) % (select count(id) from domain_roles) limit 1)                             as domain_role_id
from department_memberships mem
         join users u
              on u.id = mem.user_id
on conflict do nothing;