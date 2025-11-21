-- map previous role 0 to role with name 'Bearbeiter:in', role 1 to 'Veröffentlicher:in', role 2 to 'Fachbereichs-Administrator:in'
insert into user_role_assignments (organizational_unit_membership_id, user_role_id)
select oum.id, urr.id
from organizational_unit_memberships oum
         join user_roles urr on urr.name = case oum.role
                                               when 0 then 'Bearbeiter:in'
                                               when 1 then 'Veröffentlicher:in'
                                               when 2 then 'Fachbereichs-Administrator:in'
    end;


-- drop outdated view memberships_with_users
drop view if exists memberships_with_users;

-- drop outdated view departments_with_memberships
drop view if exists departments_with_memberships;

-- drop the legacy role column from organizational_unit_memberships table
-- and add created and updated timestamp columns
alter table organizational_unit_memberships
    drop column role,
    add column created timestamp not null default current_timestamp,
    add column updated timestamp not null default current_timestamp;