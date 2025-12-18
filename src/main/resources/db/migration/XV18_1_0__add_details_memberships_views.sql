-- create view for department memberships with details
create view v_department_memberships_with_details as
select mems.*,

       deps.name,
       deps.address,
       deps.imprint,
       deps.common_privacy,
       deps.common_accessibility,
       deps.technical_support_address,
       deps.special_support_address,
       deps.department_mail,
       deps.theme_id,
       deps.technical_support_phone,
       deps.technical_support_info,
       deps.special_support_phone,
       deps.special_support_info,
       deps.additional_info,
       deps.depth,
       deps.parent_department_id,
       deps.parent_names,
       deps.parent_ids,

       usrs.email,
       usrs.first_name,
       usrs.last_name,
       usrs.full_name,
       usrs.enabled,
       usrs.verified,
       usrs.system_role_id,
       usrs.deleted_in_idp
from department_memberships mems
         join v_departments_shadowed deps on deps.id = mems.department_id
         join users usrs on usrs.id = mems.user_id;

-- create view for team memberships with details
create view v_team_memberships_with_details as
select mems.*,

       tms.name,

       usrs.email,
       usrs.first_name,
       usrs.last_name,
       usrs.full_name,
       usrs.enabled,
       usrs.verified,
       usrs.system_role_id,
       usrs.deleted_in_idp
from team_memberships mems
         join teams tms on tms.id = mems.team_id
         join users usrs on usrs.id = mems.user_id;
