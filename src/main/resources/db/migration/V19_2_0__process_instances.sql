-- create table for process instances
create table process_instances
(
    -- The unique ID of this process instance
    id                    bigserial     not null,
    -- The key of this process instance.
    -- Public access to this process instance is done via this key.
    access_key            uuid          not null,

    -- The process definition version this instance is based on
    process_id            int           not null,

    -- The status of this process instance
    -- Options are:
    --   0 - Pending
    --   1 - Running / WorkingOn
    --   2 - Paused
    --   3 - Completed (all nodes reached an end state)
    --   4 - Aborted (by user)
    --   5 - Failed
    status                smallint      not null default 0,
    -- The status override triggered by nodes
    status_override       varchar(96)   null,

    -- The user assigned to this process instance, if any
    assigned_user_id      varchar(36)   null,

    -- A list of assigned file numbers for this process instance
    assigned_file_numbers varchar(96)[] not null default '{}',

    -- A list of identities, assigned to this process instance
    identities            jsonb         not null default '{}',

    -- The timestamp when this process instance was started
    started               timestamp     not null default now(),

    -- The timestamp when this process instance was last updated
    updated               timestamp     not null default now(),

    -- The timestamp when this process instance was finished either by completion, failure or abortion
    finished              timestamp     null,

    -- The total runtime of this process instance
    runtime               interval generated always as (finished - started) stored,

    -- The initial payload provided when starting this process instance
    initial_payload       jsonb         not null default '{}',
    -- The ID of the initial node where this process instance started
    initial_node_id       int           not null,

    -- Keep until timestamp
    keep_until            timestamp     null,

    primary key (id),
    unique (access_key),
    foreign key (process_id) references processes (id) on delete cascade,
    foreign key (initial_node_id) references process_nodes (id) on delete restrict,
    foreign key (assigned_user_id) references users (id) on delete restrict
);

create table process_instance_tasks
(
    id                       bigserial   not null,
    access_key               uuid        not null,
    process_instance_id      bigint      not null,

    process_id               int         not null,
    process_version          int         not null,
    process_node_id          int         not null,
    previous_process_node_id int         null,

    -- The status of this process task
    -- Options are:
    --   0 - Running
    --   1 - Paused
    --   2 - Waiting for retry
    --   3 - Completed (this node reached an end state)
    --   4 - Aborted (by user)
    --   5 - Failed
    status                   smallint    not null default 0,
    -- The status override triggered by nodes
    status_override          varchar(96) null,

    -- The timestamp when this task was started
    started                  timestamp   not null default now(),
    -- The timestamp when this task was last updated
    updated                  timestamp   not null default now(),
    -- The timestamp when this task was finished either by completion, failure or abortion
    finished                 timestamp   null,
    -- The total runtime of this task
    runtime                  interval    null generated always as (finished - started) stored,

    -- The data this node needs during its runtime
    runtime_data             jsonb       not null default '{}',

    -- The data this node has produced
    node_data                jsonb       not null default '{}',

    -- The global data of the overall process instance at the time this task was last updated
    process_data             jsonb       not null default '{}',

    -- The user assigned to this task, if any
    assigned_user_id         varchar(36) null,

    -- The deadline for this task, if any, determined by the time limit of the node
    deadline                 timestamp   null,

    -- The postponed until timestamp for this task, if any
    postponed_until          timestamp   null,

    -- The number of retries already attempted for this task
    retry_count              int         null     default 0,
    -- The next retry timestamp for this task, if any
    next_retry_at            timestamp   null,

    primary key (id),
    unique (process_instance_id, access_key),

    foreign key (process_instance_id) references process_instances (id) on delete cascade,
    foreign key (process_id) references processes (id) on delete cascade,
    foreign key (process_id, process_version) references process_versions (process_id, process_version) on delete restrict,
    foreign key (process_node_id) references process_nodes (id) on delete restrict,
    foreign key (assigned_user_id) references users (id) on delete restrict
);

create table process_instance_events
(
    id                       bigserial   not null,

    -- The process instance this event belongs to
    process_instance_id      bigint      not null,
    -- The process instance task this event belongs to, if any
    process_instance_task_id bigint      null,

    -- The level of this event
    -- Options are:
    --  0 - Debug
    --  1 - Info
    --  2 - Warning
    --  3 - Error
    level                    smallint    not null default 0,

    -- Whether this is a technical event not shown to end users
    is_technical             boolean     not null default false,
    -- Whether this is an audit event shown to end users
    is_audit                 boolean     not null default false,

    -- The title and message of this event
    title                    varchar(96) not null,
    message                  text        not null default '',

    -- The details of this event
    details                  jsonb       not null default '{}',

    -- The timestamp of this event
    timestamp                timestamp   not null default now(),

    -- The user who triggered this event, if any
    triggering_user_id       varchar(36) null,

    primary key (id),
    foreign key (triggering_user_id) references users (id) on delete restrict,
    foreign key (process_instance_id) references process_instances (id) on delete cascade,
    foreign key (process_instance_task_id) references process_instance_tasks (id) on delete cascade
);
