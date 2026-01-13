create table domain_roles
(
    id          serial primary key,
    name        varchar(64)   not null unique,
    description text          null,
    permissions varchar(64)[] not null default '{}',
    created     timestamp     not null default current_timestamp,
    updated     timestamp     not null default current_timestamp
);

create table domain_role_assignments
(
    id                       serial primary key,
    department_membership_id integer   null references department_memberships (id) on delete cascade,
    team_membership_id       integer   null references team_memberships (id) on delete cascade,
    domain_role_id           integer   not null references domain_roles (id) on delete cascade,
    created                  timestamp not null default current_timestamp,
    unique nulls not distinct (department_membership_id, team_membership_id, domain_role_id),
    check ( (department_membership_id is null) <> (team_membership_id is null) )
);

-- create a view to easily get users along with their domain role permissions
-- this view also takes into account deputies, so that a user gets the permissions
-- of their original user if they are a deputy
create view v_user_domain_permission as
select u.user_id                              as user_id,
       u.department_id                        as department_id,
       u.team_id                              as team_id,
       array_unique_union_agg(dr.permissions) as permissions
from (
         -- self department assignments
         select u.id               as user_id,
                dm.department_id   as department_id,
                null               as team_id,
                dma.domain_role_id as domain_role_id
         from users u
                  join department_memberships dm
                       on u.id = dm.user_id
                  join domain_role_assignments dma
                       on dm.id = dma.department_membership_id

         union all

         -- self team assignments
         select u.id               as user_id,
                null               as department_id,
                tm.team_id         as team_id,
                dma.domain_role_id as domain_role_id
         from users u
                  join team_memberships tm
                       on u.id = tm.user_id
                  join domain_role_assignments dma
                       on tm.id = dma.team_membership_id

         union all

         -- deputy department assignments
         select v.original_user_id as user_id,
                dm.department_id   as department_id,
                null               as team_id,
                dma.domain_role_id as domain_role_id
         from v_user_is_recursively_deputy_for v
                  join department_memberships dm
                       on v.original_user_id = dm.user_id
                  join domain_role_assignments dma
                       on dm.id = dma.department_membership_id

         union all

         -- deputy team assignments
         select v.original_user_id as user_id,
                null               as department_id,
                tm.team_id         as team_id,
                dma.domain_role_id as domain_role_id
         from v_user_is_recursively_deputy_for v
                  join team_memberships tm
                       on v.original_user_id = tm.user_id
                  join domain_role_assignments dma
                       on tm.id = dma.team_membership_id) as u
         join domain_roles dr
              on u.domain_role_id = dr.id
group by (u.user_id, u.department_id, u.team_id);