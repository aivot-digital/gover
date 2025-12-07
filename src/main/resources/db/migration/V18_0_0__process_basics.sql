-- create a table for process definitions.
-- process definitions contain all details for a process and are versioned.
create table process_definitions
(
    -- The ID of this process definition
    id                   serial primary key,
    -- The internal name of the definition
    name                 varchar(96) not null,
    -- The ID of the owning department who can create, edit and delete this process definition
    owning_department_id int         not null references departments (id) on delete cascade
);

-- create a table for the versions of process
create table process_definition_version
(
    -- The parent definition ID
    process_definition_id      int      not null references process_definitions (id) on delete cascade,
    -- The version of this process definition
    process_definition_version int      not null default 0,
    -- The status of this process definition.
    -- Options are:
    --   0 - Draft
    --   1 - Published
    --   2 - Revoked
    status                     smallint not null default 0,

    primary key (process_definition_id, process_definition_version)
);

-- create a table for the node definitions
create table process_node_definitions
(
    -- The ID of this particular node definition
    id                         serial primary key,

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

    foreign key (process_definition_id, process_definition_version) references process_definition_version (process_definition_id, process_definition_version) on delete cascade,
    unique (process_definition_id, process_definition_version, data_key)
);

-- create a table for the edge definitions between the nodes
create table process_edge_definitions
(
    -- The ID of this particular edge definition
    id                         serial primary key,

    -- The composite foreign for the process definition version
    process_definition_id      int         not null,
    process_definition_version int         not null,

    -- The node, this edge starts
    from_node_id               int         not null references process_node_definitions (id) on delete cascade,
    -- The node, this edge ends
    to_node_id                 int         not null references process_node_definitions (id) on delete cascade,
    -- The port, for which this edge should be used
    via_port                   varchar(32) not null,

    foreign key (process_definition_id, process_definition_version) references process_definition_version (process_definition_id, process_definition_version) on delete cascade,
    unique (from_node_id, to_node_id, via_port)
);