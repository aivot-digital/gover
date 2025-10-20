create table data_object_schemas
(
    key         varchar(64) not null,
    name        varchar(96) not null,
    description text        not null,
    id_gen      varchar(64) not null,
    schema      jsonb       not null,
    created     timestamp   not null default current_timestamp,
    updated     timestamp   not null default current_timestamp,
    primary key (key)
);

create table data_object_items
(
    schema_key varchar(64) not null,
    id         varchar(64) not null,
    data       jsonb       not null,
    created     timestamp   not null default current_timestamp,
    updated     timestamp   not null default current_timestamp,
    primary key (schema_key, id),
    foreign key (schema_key) references data_object_schemas (key) on delete cascade on update cascade
);
