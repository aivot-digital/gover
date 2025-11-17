-- Drop outdated view if it exists
drop view if exists departments_with_memberships;

-- Recreate the view with updated logic
create view org_units_with_memberships as
select deps.id,
       deps.name,
       deps.address,
       deps.imprint,
       deps.privacy,
       deps.accessibility,
       deps.technical_support_address,
       deps.special_support_address,
       deps.contact_legal,
       deps.contact_technical,
       deps.additional_info,
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
from organizational_units_shadowed as deps
         join organizational_unit_memberships as mems
              on deps.id = mems.organizational_unit_id
         join users as usrs
              on usrs.id = mems.user_id;
