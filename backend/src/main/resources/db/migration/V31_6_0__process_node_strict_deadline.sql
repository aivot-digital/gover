alter table process_nodes
    add column time_limit_strict boolean not null default false;
