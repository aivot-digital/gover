-- add logo and favicon to themes
alter table themes
    add column logo_key    uuid null references assets (key) on delete set null,
    add column favicon_key uuid null references assets (key) on delete set null;

-- set keys for system media for system theme
update themes
set logo_key    = (select value from system_configs where key = 'SystemLogo' limit 1)::uuid,
    favicon_key = (select value from system_configs where key = 'SystemFavicon' limit 1)::uuid
where id = (select value from system_configs where key = 'SystemTheme' limit 1)::int;

-- add theme_id to departments
alter table departments
    add column theme_id int null references themes (id) on delete set null;

-- recreate views to include theme_id in departments
drop view departments_with_memberships;
create view departments_with_memberships as
select deps.id,
       deps.name,
       deps.address,
       deps.imprint,
       deps.privacy,
       deps.accessibility,
       deps.technical_support_address,
       deps.special_support_address,
       deps.department_mail,
       deps.theme_id,
       deps.created,
       deps.updated,
       mems.id             as membership_id,
       mems.role           as membership_role,
       usrs.id             as user_id,
       usrs.email          as user_email,
       usrs.first_name     as user_first_name,
       usrs.last_name      as user_last_name,
       usrs.full_name      as user_full_name,
       usrs.enabled        as user_enabled,
       usrs.verified       as user_verified,
       usrs.global_admin   as user_global_admin,
       usrs.deleted_in_idp as user_deleted_in_idp
from departments as deps
         join department_memberships as mems
              on deps.id = mems.department_id
         join users as usrs
              on usrs.id = mems.user_id;