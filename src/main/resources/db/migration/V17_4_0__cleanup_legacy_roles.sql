-- drop outdated views that included legacy role information
drop view submissions_with_memberships;
drop view form_versions_with_memberships;
drop view form_with_memberships;
drop view memberships_with_users;
drop view departments_with_memberships;

-- migrate legacy global admin users to use system role
update users
set system_role_id = 1
where global_admin = true;

-- drop user global admin column
alter table users
    drop column global_admin;

-- TODO: Migrate old department admin roles to new domain roles before dropping the legacy role column

-- drop the legacy role column from organizational_unit_memberships table
-- and add created and updated timestamp columns
alter table department_memberships
    drop column role,
    add column created timestamp not null default current_timestamp,
    add column updated timestamp not null default current_timestamp;
