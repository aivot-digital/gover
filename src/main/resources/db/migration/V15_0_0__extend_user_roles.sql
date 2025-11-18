create table user_roles
(
    id                          serial primary key,
    name                        varchar(64) not null unique,
    description                 text,
    form_permission             integer     not null,
    process_permission          integer     not null default 0,
    process_instance_permission integer     not null default 0,
    created                     timestamp   not null default current_timestamp,
    updated                     timestamp   not null default current_timestamp
);

create table user_role_assignments
(
    id                                serial primary key,
    organizational_unit_membership_id integer   null references organizational_unit_memberships (id) on delete cascade,
    team_membership_id                integer   null references team_memberships (id) on delete cascade,
    user_role_id                      integer   not null references user_roles (id) on delete cascade,
    created                           timestamp not null default current_timestamp,
    unique nulls not distinct (organizational_unit_membership_id, team_membership_id, user_role_id),
    check ( (organizational_unit_membership_id is null) <> (team_membership_id is null) )
);