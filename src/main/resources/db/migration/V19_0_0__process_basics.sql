-- Create a table for process definitions.
-- Process definitions contain all details for a process and are versioned.
create table processes
(
    -- The ID of this process definition
    id                serial      not null,
    -- The internal name of the definition
    internal_title    varchar(96) not null,

    -- The ID of the owning department who can create, edit and delete this process definition
    department_id     int         not null,

    -- The public key of this process.
    -- Public access to this process is done via this key.
    access_key        uuid        not null,

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
create table process_versions
(
    -- The parent definition ID
    process_id      int          not null,
    -- The version of this process definition
    process_version int          not null default 0,
    -- The status of this process definition.
    -- Options are:
    --   0 - Drafted
    --   1 - Published
    --   2 - Revoked
    status          smallint     not null default 0,

    -- The public title of this process definition version
    public_title    varchar(192) not null,

    -- Timestamps for creation and last update
    created         timestamp    not null default now(),
    updated         timestamp    not null default now(),
    published       timestamp    null,
    revoked         timestamp    null,

    -- Define the composite primary key
    primary key (process_id, process_version),
    -- Define the foreign key to the process definition
    foreign key (process_id) references processes (id) on delete cascade
);

-- Create a table for the node definitions
create table process_nodes
(
    -- The ID of this particular node definition
    id                              serial       not null,

    -- The composite foreign for the process definition version
    process_id                      int          not null,
    process_version                 int          not null,

    -- The name of this node
    name                            varchar(96)  null,
    -- A short description of this node
    description                     text         null,

    -- The key, the data of this node are stored in the process instance context
    data_key                        varchar(32)  not null,

    -- The key and version for the process node definition plugin component
    process_node_definition_key     varchar(128) not null,
    process_node_definition_version integer      not null,

    -- The configuration object for this node.
    -- All options are stored in here.
    configuration                   jsonb        not null default '{}',

    -- The output mappings for this node.
    output_mappings                 jsonb        not null default '{}',

    -- The timelimit in days for this node.
    time_limit_days                 integer      null,

    -- The requirements for this node.
    requirements                    text         null,

    -- Additional notes for this node.
    notes                           text         null,

    -- Flag to determine if this node was saved with errors.
    -- This is used to display a warning in the UI and to prevent publishing of process definitions with errors.
    saved_with_errors               bool         not null default false,

    -- Define the primary key
    primary key (id),
    -- Define the foreign key to the process definition
    foreign key (process_id) references processes (id) on delete cascade,
    -- Define the foreign key to the process definition version
    foreign key (process_id, process_version) references process_versions (process_id, process_version) on delete cascade,
    -- Ensure that each data_key is only used once per process definition version
    unique (process_id, process_version, data_key)
);

-- Create a table for the edge definitions between the nodes
create table process_edges
(
    -- The ID of this particular edge definition
    id              serial      not null,

    -- The composite foreign for the process definition version
    process_id      int         not null,
    process_version int         not null,

    -- The node, this edge starts
    from_node_id    int         not null,
    -- The node, this edge ends
    to_node_id      int         not null,
    -- The port, for which this edge should be used
    via_port        varchar(32) not null,

    -- Define the primary key
    primary key (id),
    -- Define the foreign key to the process definition
    foreign key (process_id) references processes (id) on delete cascade,
    -- Define the foreign key to the process definition version
    foreign key (process_id, process_version) references process_versions (process_id, process_version) on delete cascade,
    -- Define the foreign key to the node this edge starts from
    foreign key (from_node_id) references process_nodes (id) on delete cascade,
    -- Define the foreign key to the node this edge ends at
    foreign key (to_node_id) references process_nodes (id) on delete cascade,
    -- Ensure that each from_node_id + via_port combination is only used once per process definition version.
    -- This ensures that from a given node and port, there is only one possible outgoing edge
    unique (from_node_id, via_port)
);
