-- create view with permissions for organizational unit memberships
create view v_department_memberships_with_permissions as
select oum.*,
       array_unique_union_agg(ur.permissions) as permissions
from department_memberships oum
         join domain_role_assignments ura
              on oum.id = ura.department_membership_id
         join domain_roles ur on ura.domain_role_id = ur.id
group by oum.id, oum.department_id, oum.user_id;

-- create view with permissions for team memberships
create view v_team_memberships_with_permissions as
select tm.*,
       array_unique_union_agg(ur.permissions) as permissions
from team_memberships tm
         join domain_role_assignments ura
              on tm.id = ura.team_membership_id
         join domain_roles ur on ura.domain_role_id = ur.id
group by tm.id, tm.team_id, tm.user_id;

-- create combined view with permissions for all memberships
create view v_memberships_with_permissions as
select memberships.department_id                    as department_id,
       memberships.team_id                          as team_id,
       memberships.user_id                          as user_id,
       array_unique_union_agg(memberships.permissions) as permissions
from (select oum.department_id as department_id,
             null              as team_id,
             oum.user_id       as user_id,
             oum.permissions   as permissions
      from v_department_memberships_with_permissions oum
      union
      select null           as department_id,
             tm.team_id     as team_id,
             tm.user_id     as user_id,
             tm.permissions as permissions
      from v_team_memberships_with_permissions tm) as memberships
group by memberships.team_id, memberships.department_id, memberships.user_id;
