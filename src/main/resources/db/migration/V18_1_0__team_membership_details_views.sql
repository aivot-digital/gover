-- create a view to show deputy team memberships with details from
-- 1. the user
-- 2. the team
-- 3. the domain roles

create view v_team_memberships_with_details as
with aggregated_roles as (select dra.team_membership_id                 as team_membership_id,
                                 jsonb_agg(dr)                          as domain_roles,
                                 array_unique_union_agg(dr.permissions) as domain_role_permissions
                          from domain_role_assignments dra
                                   join domain_roles dr on dr.id = dra.domain_role_id
                          group by dra.team_membership_id)
select mem.id                     as membership_id,
       mem.is_deputy              as membership_is_deputy,
       mem.as_deputy_for_user_id  as membership_as_deputy_for_user_id,
       deputy_usr.email           as membership_as_deputy_for_user_email,
       deputy_usr.first_name      as membership_as_deputy_for_user_first_name,
       deputy_usr.last_name       as membership_as_deputy_for_user_last_name,
       deputy_usr.full_name       as membership_as_deputy_for_user_full_name,
       deputy_usr.enabled         as membership_as_deputy_for_user_enabled,
       deputy_usr.verified        as membership_as_deputy_for_user_verified,
       deputy_usr.deleted_in_idp  as membership_as_deputy_for_user_deleted_in_idp,
       deputy_usr.system_role_id  as membership_as_deputy_for_user_system_role_id,

       usr.id                     as user_id,
       usr.email                  as user_email,
       usr.first_name             as user_first_name,
       usr.last_name              as user_last_name,
       usr.full_name              as user_full_name,
       usr.enabled                as user_enabled,
       usr.verified               as user_verified,
       usr.deleted_in_idp         as user_deleted_in_idp,
       usr.system_role_id         as user_system_role_id,

       team.id                    as team_id,
       team.name                  as team_name,

       ar.domain_roles            as domain_roles,
       ar.domain_role_permissions as domain_role_permissions
from v_deputy_team_memberships mem
         join teams team on team.id = mem.team_id
         join users usr on usr.id = mem.user_id
         left join aggregated_roles ar on ar.team_membership_id = mem.id
         left join users deputy_usr on deputy_usr.id = mem.as_deputy_for_user_id;