create table resource_access_controls
(
    id                                   serial primary key,
    source_team_id                       integer   null references teams (id) on delete cascade,
    source_department_id                 integer   null references departments (id) on delete cascade,

    target_form_id                       integer   null references forms (id) on delete cascade,
    target_process_id                    integer   null references forms (id) on delete cascade,
    target_process_instance_id           integer   null references forms (id) on delete cascade,

    form_permission_create               bool      not null default false,
    form_permission_read                 bool      not null default false,
    form_permission_edit                 bool      not null default false,
    form_permission_delete               bool      not null default false,
    form_permission_annotate             bool      not null default false,
    form_permission_publish              bool      not null default false,

    process_permission_create            bool      not null default false,
    process_permission_read              bool      not null default false,
    process_permission_edit              bool      not null default false,
    process_permission_delete            bool      not null default false,
    process_permission_annotate          bool      not null default false,
    process_permission_publish           bool      not null default false,

    process_instance_permission_create   bool      not null default false,
    process_instance_permission_read     bool      not null default false,
    process_instance_permission_edit     bool      not null default false,
    process_instance_permission_delete   bool      not null default false,
    process_instance_permission_annotate bool      not null default false,

    created                              timestamp not null default current_timestamp,
    updated                              timestamp not null default current_timestamp,

    check ( (source_team_id is null) <> (source_department_id is null) ),
    check (
        (case when target_form_id is not null then 1 else 0 end) +
        (case when target_process_id is not null then 1 else 0 end) +
        (case when target_process_instance_id is not null then 1 else 0 end)
            = 1
        )
);
