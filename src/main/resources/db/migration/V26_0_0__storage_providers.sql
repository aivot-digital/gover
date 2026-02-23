create table storage_providers
(
    id                                  serial primary key,
    storage_provider_definition_key     varchar(255) not null,
    storage_provider_definition_version integer      not null,
    name                                varchar(255) not null,
    description                         text         not null,
    type                                smallint     not null,
    status                              smallint     not null default 0,
    status_message                      text         null,
    read_only                           boolean      not null default false,
    configuration                       jsonb        not null default '{}',
    max_file_size_in_bytes              bigint       not null default 0,
    prevent_deletion                    boolean      not null default false,
    metadata_attributes                 jsonb        not null default '{}',
    last_sync                           timestamp    null,
    created                             timestamp    not null default now(),
    updated                             timestamp    not null default now()
);

create table storage_index_items
(
    storage_provider_id   integer      not null references storage_providers (id) on delete cascade,
    storage_provider_type smallint     not null,
    path_from_root        text         not null,
    directory             boolean      not null,
    filename              varchar(255) not null,
    mime_type             varchar(255) null,
    missing               boolean      not null default false,
    metadata              jsonb        not null,
    created               timestamp    not null default now(),
    updated               timestamp    not null default now(),
    primary key (storage_provider_id, path_from_root)
);
