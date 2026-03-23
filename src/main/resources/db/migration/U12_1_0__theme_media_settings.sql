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

alter table themes
    drop column logo_key,
    drop column favicon_key;

alter table departments
    drop column theme_id;

delete from flyway_schema_history where version = '12.1.0'