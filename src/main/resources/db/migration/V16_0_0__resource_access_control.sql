create table resource_access_controls
(
    id                         serial primary key,
    source_team_id             integer   null references teams (id) on delete cascade,
    source_org_unit_id         integer   null references organizational_units (id) on delete cascade,

    target_form_id             integer   null references forms (id) on delete cascade,
    target_process_id          integer   null references forms (id) on delete cascade,
    target_process_instance_id integer   null references forms (id) on delete cascade,

    access_type                integer   not null default 0, -- 0: read, 1: write
    created                    timestamp not null default current_timestamp,
    updated                    timestamp not null default current_timestamp,

    check ( (source_team_id is null) <> (source_org_unit_id is null) ),
    check ( ((target_form_id is null) <> (target_process_id is null)) <> (target_process_instance_id is null) )
);
