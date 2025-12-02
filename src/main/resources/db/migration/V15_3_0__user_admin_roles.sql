-- add global_role column to users table
alter table users
    add column global_role integer not null default 0; -- 0: regular user, 1: systemadmin, 2: superadmin

-- update existing global admins to superadmin role
update users
set global_role = 2
where global_admin = true;

-- drop user global admin dependent views
drop view submissions_with_memberships;
drop view form_versions_with_memberships;
drop view form_with_memberships;

-- drop user global admin column
alter table users
    drop column global_admin;
