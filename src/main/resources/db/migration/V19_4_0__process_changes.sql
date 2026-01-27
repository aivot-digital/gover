-- create a table to track changes to process definitions
create table process_changes
(
    -- The unique ID of this change
    id              bigserial    not null,
    -- The timestamp of this change
    timestamp       timestamp    not null default now(),
    -- The user who made this change
    user_id         varchar(36)  not null,

    -- The process definition this change belongs to
    process_id      int          not null,
    -- The version of the process definition this change belongs to
    process_version int          null,
    -- The node this change belongs to, if any
    process_node_id int          null,
    -- The edge this change belongs to, if any
    process_edge_id int          null,

    -- The type of change
    -- Options are:
    --   0 - Created
    --   1 - Updated
    --   2 - Deleted
    change_type     smallint     not null,
    -- The diff object describing the change
    diff            jsonb        not null default '{}',
    -- An optional comment describing this change
    comment         varchar(512) null,

    -- Define the primary key and foreign keys
    primary key (id),
    foreign key (user_id) references users (id) on delete restrict,
    foreign key (process_id) references processes (id) on delete cascade,
    foreign key (process_id, process_version) references process_versions (process_id, process_version) on delete cascade,
    foreign key (process_node_id) references process_nodes (id) on delete cascade,
    foreign key (process_edge_id) references process_edges (id) on delete cascade
);