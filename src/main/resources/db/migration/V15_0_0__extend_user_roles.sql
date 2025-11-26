create table user_roles
(
    id                                   serial primary key,
    name                                 varchar(64) not null unique,
    description                          text,

    department_permission_edit           bool        not null default false,
    team_permission_edit                 bool        not null default false,

    form_permission_create               bool        not null default false,
    form_permission_read                 bool        not null default false,
    form_permission_edit                 bool        not null default false,
    form_permission_delete               bool        not null default false,
    form_permission_annotate             bool        not null default false,
    form_permission_publish              bool        not null default false,

    process_permission_create            bool        not null default false,
    process_permission_read              bool        not null default false,
    process_permission_edit              bool        not null default false,
    process_permission_delete            bool        not null default false,
    process_permission_annotate          bool        not null default false,
    process_permission_publish           bool        not null default false,

    process_instance_permission_create   bool        not null default false,
    process_instance_permission_read     bool        not null default false,
    process_instance_permission_edit     bool        not null default false,
    process_instance_permission_delete   bool        not null default false,
    process_instance_permission_annotate bool        not null default false,

    created                              timestamp   not null default current_timestamp,
    updated                              timestamp   not null default current_timestamp
);

create table user_role_assignments
(
    id                       serial primary key,
    department_membership_id integer   null references department_memberships (id) on delete cascade,
    team_membership_id       integer   null references team_memberships (id) on delete cascade,
    user_role_id             integer   not null references user_roles (id) on delete cascade,
    created                  timestamp not null default current_timestamp,
    unique nulls not distinct (department_membership_id, team_membership_id, user_role_id),
    check ( (department_membership_id is null) <> (team_membership_id is null) )
);
