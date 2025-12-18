-- create a table for the system roles
create table system_roles
(
    id          serial primary key,
    name        varchar(64)   not null unique,
    description text          null,
    permissions varchar(64)[] not null default '{}',
    created     timestamp     not null default current_timestamp,
    updated     timestamp     not null default current_timestamp,
    unique (name)
);

-- add a foreign key column to users table to reference system roles
alter table users
    add column system_role_id integer null references system_roles (id) on delete set null;

-- create a view to easily get users along with their system role permissions
create view v_user_system_permission as
select u.id                                   as user_id,
       array_unique_union_agg(sr.permissions) as permissions
from (select u.id as id,
             u.system_role_id
      from users u

      union all

      select u_original.id as id,
             sr.id         as system_role_id
      from v_user_is_recursively_deputy_for v
               join users u_original
                    on v.original_user_id = u_original.id
               join system_roles sr
                    on u_original.system_role_id = sr.id) as u

         join system_roles sr
              on u.system_role_id = sr.id
group by (u.id);