-- create a table for teams to group users
create table teams
(
    id      serial primary key,
    name    varchar(64) not null unique,
    created timestamp   not null default current_timestamp,
    updated timestamp   not null default current_timestamp
);

-- create a table for team memberships to link users to teams
create table team_memberships
(
    id      serial primary key,
    team_id integer     not null references teams (id) on delete cascade,
    user_id varchar(36) not null references users (id) on delete cascade,
    created timestamp   not null default current_timestamp,
    updated timestamp   not null default current_timestamp,
    unique (team_id, user_id)
);