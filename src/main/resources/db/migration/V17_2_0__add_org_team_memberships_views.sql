-- create view for organization unit memberships with details
create view v_organizational_unit_memberships_with_details as
select mem.id                as membership_id,

       ou.id                 as organizational_unit_id,
       ou.name               as organizational_unit_name,
       ou.parent_org_unit_id as organizational_unit_parent_org_unit_id,
       ou.depth              as organizational_unit_depth,

       u.id                  as user_id,
       u.first_name     as user_first_name,
       u.last_name      as user_last_name,
       u.full_name           as user_full_name,
       u.email               as user_email,
       u.enabled             as user_enabled,
       u.verified            as user_verified,
       u.global_admin        as user_global_admin,
       u.deleted_in_idp      as user_deleted_in_idp
from organizational_unit_memberships mem
         join organizational_units ou on ou.id = mem.organizational_unit_id
         join users u on u.id = mem.user_id;

-- create view for team memberships with details
create view v_team_memberships_with_details as
select mem.id           as membership_id,

       t.id             as team_id,
       t.name           as team_name,

       u.id             as user_id,
       u.first_name     as user_first_name,
       u.last_name      as user_last_name,
       u.full_name      as user_full_name,
       u.email          as user_email,
       u.enabled        as user_enabled,
       u.verified       as user_verified,
       u.global_admin   as user_global_admin,
       u.deleted_in_idp as user_deleted_in_idp
from team_memberships mem
         join teams t on t.id = mem.team_id
         join users u on u.id = mem.user_id;
