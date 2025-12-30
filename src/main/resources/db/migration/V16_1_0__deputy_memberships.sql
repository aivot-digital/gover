-- create a view that shows department memberships including deputy memberships
create view v_deputy_department_memberships as
select dep.id            as id,
       dep.department_id as department_id,
       dep.user_id       as user_id,
       true              as is_original_member,
       false             as is_deputy,
       null              as as_deputy_for_user_id
from department_memberships dep

union all

select dep.id              as id,
       dep.department_id   as department_id,
       ud.deputy_user_id   as user_id,
       false               as is_original_member,
       true                as is_deputy,
       ud.original_user_id as as_deputy_for_user_id
from department_memberships dep
         join v_user_is_recursively_deputy_for ud
              on dep.user_id = ud.original_user_id;

-- create a view that shows team memberships including deputy memberships
create view v_deputy_team_memberships as
select tm.id      as id,
       tm.team_id as team_id,
       tm.user_id as user_id,
       true       as is_original_member,
       false      as is_deputy,
       null       as as_deputy_for_user_id
from team_memberships tm

union all

select tm.id               as id,
       tm.team_id          as team_id,
       ud.deputy_user_id   as user_id,
       false               as is_original_member,
       true                as is_deputy,
       ud.original_user_id as as_deputy_for_user_id
from team_memberships tm
         join v_user_is_recursively_deputy_for ud
              on tm.user_id = ud.original_user_id;