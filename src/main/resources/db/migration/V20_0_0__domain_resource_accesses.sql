-- create a table to control the access of teams/departments to processes
create table process_access_controls
(
    id                   serial primary key,
    source_team_id       integer       null references teams (id) on delete cascade,
    source_department_id integer       null references departments (id) on delete cascade,

    target_process_id    integer       not null references processes (id) on delete cascade,

    permissions          varchar(64)[] not null default '{}',

    created              timestamp     not null default current_timestamp,
    updated              timestamp     not null default current_timestamp,

    -- ensure that either source_team_id or source_department_id is set, but not both
    check ( (source_team_id is null) <> (source_department_id is null) ),

    -- ensure uniqueness of source and target combinations
    unique (source_team_id, source_department_id, target_process_id)
);

-- create a table for presets to control the access of teams/departments to process instances
create table process_instance_access_control_presets
(
    id                     serial primary key,
    source_team_id         integer       null references teams (id) on delete cascade,
    source_department_id   integer       null references departments (id) on delete cascade,

    target_process_id      integer       not null,
    target_process_version integer       not null,

    permissions            varchar(64)[] not null default '{}',

    created                timestamp     not null default current_timestamp,
    updated                timestamp     not null default current_timestamp,

    -- ensure that either source_team_id or source_department_id is set, but not both
    check ( (source_team_id is null) <> (source_department_id is null) ),

    -- ensure uniqueness of source and target combinations
    unique (source_team_id, source_department_id, target_process_id),

    -- ensure that target_process_id and target_process_version refer to a valid process version
    foreign key (target_process_id, target_process_version) references process_versions (process_id, process_version) on delete cascade
);

-- create a table for presets to control the access of teams/departments to process instances and their tasks
create table process_instance_access_controls
(
    id                         serial primary key,
    source_team_id             integer       null references teams (id) on delete cascade,
    source_department_id       integer       null references departments (id) on delete cascade,

    target_process_instance_id integer       not null references process_instances (id) on delete cascade,

    permissions                varchar(64)[] not null default '{}',

    created                    timestamp     not null default current_timestamp,
    updated                    timestamp     not null default current_timestamp,

    -- ensure that either source_team_id or source_department_id is set, but not both
    check ( (source_team_id is null) <> (source_department_id is null) ),

    -- ensure uniqueness of source and target combinations
    unique (source_team_id, source_department_id, target_process_instance_id)
);

-- create a function to copy permissions from presets to actual process instance access controls
create function handle_process_instance_insert()
    returns trigger
    language plpgsql
as
$$
begin
    insert into process_instance_access_controls (source_team_id,
                                                  source_department_id,
                                                  target_process_instance_id,
                                                  permissions,
                                                  created,
                                                  updated)
    select piacp.source_team_id       as source_team_id,
           piacp.source_department_id as source_department_id,
           NEW.id                     as target_process_instance_id,
           piacp.permissions          as permissions,
           current_timestamp          as created,
           current_timestamp          as updated
    from process_instance_access_control_presets piacp
    where target_process_id = NEW.process_id;
    return NEW;
end;
$$;

-- create a trigger
create trigger on_process_instance_insert
    after insert
    on process_instances
    for each row
execute function handle_process_instance_insert();
