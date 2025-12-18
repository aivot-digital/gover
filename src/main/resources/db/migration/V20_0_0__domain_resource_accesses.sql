-- create a table to control the access of teams/departments to process definitions/instances/tasks
create table process_access_controls
(
    id                              serial primary key,
    source_team_id                  integer       null references teams (id) on delete cascade,
    source_department_id            integer       null references departments (id) on delete cascade,

    target_process_definition_id    integer       not null references process_definitions (id) on delete cascade,
    target_process_instance_id      integer       null references process_instances (id) on delete cascade,
    target_process_instance_task_id integer       null references process_instance_tasks (id) on delete cascade,

    permissions                     varchar(64)[] not null default '{}',

    created                         timestamp     not null default current_timestamp,
    updated                         timestamp     not null default current_timestamp,

    -- ensure that either source_team_id or source_department_id is set, but not both
    check ( (source_team_id is null) <> (source_department_id is null) ),

    -- ensure that if target_process_instance_task_id is set, target_process_instance_id is also set
    check ( (target_process_instance_task_id is null) or (target_process_instance_id is not null))
);
