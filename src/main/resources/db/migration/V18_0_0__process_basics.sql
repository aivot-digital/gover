-- Create a table for process definitions.
-- Process definitions contain all details for a process and are versioned.
create table process_definitions
(
    -- The ID of this process definition
    id                serial      not null,
    -- The internal name of the definition
    name              varchar(96) not null,
    -- A short description of this process definition
    description       text        null,
    -- The ID of the owning department who can create, edit and delete this process definition
    department_id     int         not null,

    -- The total count of versions for this process definition
    version_count     int         not null default 0,
    -- The currently drafted version number for this process definition
    drafted_version   int         null,
    -- The currently published version number for this process definition
    published_version int         null,

    -- Timestamps for creation and last update
    created           timestamp   not null default now(),
    updated           timestamp   not null default now(),

    -- Define the primary key
    primary key (id),
    -- Define the foreign key to the department
    foreign key (department_id) references departments (id) on delete cascade
);

-- Create a table for the versions of process.
-- Each process definition can have multiple versions.
create table process_definition_versions
(
    -- The parent definition ID
    process_definition_id      int       not null,
    -- The version of this process definition
    process_definition_version int       not null default 0,
    -- The status of this process definition.
    -- Options are:
    --   0 - Draft
    --   1 - Published
    --   2 - Revoked
    status                     smallint  not null default 0,

    -- Define the retention time unit for process instances of this definition.
    -- Options are:
    --   0 - Days
    --   1 - Weeks
    --   2 - Months
    --   3 - Years
    retention_time_unit        smallint  not null default 0,
    -- Define the retention time amount for process instances of this definition.
    retention_time_amount      int       not null default 30,

    -- Timestamps for creation and last update
    created                    timestamp not null default now(),
    updated                    timestamp not null default now(),
    published                  timestamp null,
    revoked                    timestamp null,

    -- Define the composite primary key
    primary key (process_definition_id, process_definition_version),
    -- Define the foreign key to the process definition
    foreign key (process_definition_id) references process_definitions (id) on delete cascade
);

-- Create a table for the node definitions
create table process_definition_nodes
(
    -- The ID of this particular node definition
    id                         serial      not null,

    -- The composite foreign for the process definition version
    process_definition_id      int         not null,
    process_definition_version int         not null,

    -- The key, the data of this node are stored in the process instance context
    data_key                   varchar(32) not null,

    -- The key for the Code definition of this node
    code_key                   varchar(32) not null,

    -- The configuration object for this node.
    -- All options are stored in here.
    configuration              jsonb       not null default '{}',

    -- Define the primary key
    primary key (id),
    -- Define the foreign key to the process definition
    foreign key (process_definition_id) references process_definitions (id) on delete cascade,
    -- Define the foreign key to the process definition version
    foreign key (process_definition_id, process_definition_version) references process_definition_versions (process_definition_id, process_definition_version) on delete cascade,
    -- Ensure that each data_key is only used once per process definition version
    unique (process_definition_id, process_definition_version, data_key)
);

-- Create a table for the edge definitions between the nodes
create table process_definition_edges
(
    -- The ID of this particular edge definition
    id                         serial      not null,

    -- The composite foreign for the process definition version
    process_definition_id      int         not null,
    process_definition_version int         not null,

    -- The node, this edge starts
    from_node_id               int         not null,
    -- The node, this edge ends
    to_node_id                 int         not null,
    -- The port, for which this edge should be used
    via_port                   varchar(32) not null,

    -- Define the primary key
    primary key (id),
    -- Define the foreign key to the process definition
    foreign key (process_definition_id) references process_definitions (id) on delete cascade,
    -- Define the foreign key to the process definition version
    foreign key (process_definition_id, process_definition_version) references process_definition_versions (process_definition_id, process_definition_version) on delete cascade,
    -- Define the foreign key to the node this edge starts from
    foreign key (from_node_id) references process_definition_nodes (id) on delete cascade,
    -- Define the foreign key to the node this edge ends at
    foreign key (to_node_id) references process_definition_nodes (id) on delete cascade,
    -- Ensure that each from_node_id + via_port combination is only used once per process definition version.
    -- This ensures that from a given node and port, there is only one possible outgoing edge
    unique (from_node_id, via_port)
);

-- create a table to track changes to process definitions
create table process_definition_changes
(
    -- The unique ID of this change
    id                         bigserial    not null,
    -- The timestamp of this change
    timestamp                  timestamp    not null default now(),
    -- The user who made this change
    user_id                    varchar(36)  not null,

    -- The process definition this change belongs to
    process_definition_id      int          not null,
    -- The version of the process definition this change belongs to
    process_definition_version int          null,
    -- The node this change belongs to, if any
    process_definition_node_id int          null,
    -- The edge this change belongs to, if any
    process_definition_edge_id int          null,

    -- The type of change
    -- Options are:
    --   0 - Created
    --   1 - Updated
    --   2 - Deleted
    change_type                smallint     not null,
    -- The diff object describing the change
    diff                       jsonb        not null default '{}',
    -- An optional comment describing this change
    comment                    varchar(512) null,

    -- Define the primary key and foreign keys
    primary key (id),
    foreign key (user_id) references users (id) on delete restrict,
    foreign key (process_definition_id) references process_definitions (id) on delete cascade,
    foreign key (process_definition_id, process_definition_version) references process_definition_versions (process_definition_id, process_definition_version) on delete cascade,
    foreign key (process_definition_node_id) references process_definition_nodes (id) on delete cascade,
    foreign key (process_definition_edge_id) references process_definition_edges (id) on delete cascade
);

-- create table for process instances
create table process_instances
(
    -- The unique ID of this process instance
    id                         bigserial     not null,
    -- The key of this process instance.
    -- Public access to this process instance is done via this key.
    access_key                 uuid          not null,

    -- The process definition version this instance is based on
    process_definition_id      int           not null,
    process_definition_version int           not null,

    -- The status of this process instance
    -- Options are:
    --   0 - Created
    --   1 - Running
    --   2 - Paused
    --   3 - Completed (all nodes reached an end state)
    --   4 - Aborted (by user)
    --   5 - Failed
    status                     smallint      not null default 0,
    -- The status override triggered by nodes
    status_override            varchar(96)   null,

    -- A list of assigned file numbers for this process instance
    assigned_file_numbers      varchar(96)[] not null default '{}',

    -- A list of delivery channels
    delivery_channels          jsonb         not null default '{}',

    -- A list of tags assigned to this process instance
    tags                       varchar(64)[] not null default '{}',

    -- The timestamp when this process instance was started
    started                    timestamp     not null default now(),

    -- The timestamp when this process instance was last updated
    updated                    timestamp     not null default now(),

    -- The timestamp when this process instance was finished either by completion, failure or abortion
    finished                   timestamp     null,

    -- The total runtime of this process instance
    runtime                    interval generated always as (finished - started) stored,

    -- The initial payload provided when starting this process instance
    initial_payload            jsonb         not null default '{}',
    -- The ID of the initial node where this process instance started
    initial_node_id            int           not null,

    primary key (id),
    unique (access_key),
    foreign key (process_definition_id) references process_definitions (id) on delete cascade,
    foreign key (process_definition_id, process_definition_version) references process_definition_versions (process_definition_id, process_definition_version) on delete restrict,
    foreign key (initial_node_id) references process_definition_nodes (id) on delete restrict
);

create table process_instance_tasks
(
    id                                  bigserial   not null,
    access_key                          uuid        not null,
    process_instance_id                 bigint      not null,

    process_definition_id               int         not null,
    process_definition_version          int         not null,
    process_definition_node_id          int         not null,
    previous_process_definition_node_id int         null,

    -- The status of this process task
    -- Options are:
    --   0 - Running
    --   1 - Paused
    --   2 - Completed (all nodes reached an end state)
    --   3 - Aborted (by user)
    --   4 - Failed
    status                              smallint    not null default 0,
    -- The status override triggered by nodes
    status_override                     varchar(96) null,

    -- The timestamp when this task was started
    started                             timestamp   not null default now(),
    -- The timestamp when this task was last updated
    updated                             timestamp   not null default now(),
    -- The timestamp when this task was finished either by completion, failure or abortion
    finished                            timestamp   null,
    -- The total runtime of this task
    runtime                             interval    null generated always as (finished - started) stored,

    -- The buffer data for this task which will be used during processing
    buffer_data                         jsonb       not null default '{}',

    -- The meta data this task has produced
    meta_data                           jsonb       not null default '{}',

    -- The working data this task has produced
    working_data                        jsonb       not null default '{}',

    -- The user assigned to this task, if any
    assigned_user_id                    varchar(36) null,

    primary key (id),
    unique (process_instance_id, access_key),

    foreign key (process_instance_id) references process_instances (id) on delete cascade,
    foreign key (process_definition_id) references process_definitions (id) on delete cascade,
    foreign key (process_definition_id, process_definition_version) references process_definition_versions (process_definition_id, process_definition_version) on delete restrict,
    foreign key (process_definition_node_id) references process_definition_nodes (id) on delete restrict,
    foreign key (assigned_user_id) references users (id) on delete restrict
);

create table process_instance_history_events
(
    id                       bigserial   not null,

    timestamp                timestamp   not null default now(),

    triggering_user_id       varchar(36) null,

    process_instance_id      bigint      not null,
    process_instance_task_id bigint      null,

    primary key (id),
    foreign key (triggering_user_id) references users (id) on delete restrict,
    foreign key (process_instance_id) references process_instances (id) on delete cascade,
    foreign key (process_instance_task_id) references process_instance_tasks (id) on delete cascade
);

create table process_instance_attachments
(
    key                      uuid        not null,
    process_instance_id      bigint      not null,
    process_instance_task_id bigint      null,

    uploaded_by_user_id      varchar(36) not null,
    uploaded_at              timestamp   not null default now(),

    primary key (key),
    foreign key (process_instance_id) references process_instances (id) on delete cascade,
    foreign key (process_instance_task_id) references process_instance_tasks (id) on delete cascade,
    foreign key (uploaded_by_user_id) references users (id) on delete restrict
);