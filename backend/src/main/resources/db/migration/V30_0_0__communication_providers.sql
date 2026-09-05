create table communication_providers
(
    id                                        serial primary key,
    communication_provider_definition_key     varchar(255) not null,
    communication_provider_definition_version integer      not null,
    name                                      varchar(64)  not null,
    description                               varchar(255) not null,
    configuration                             jsonb        not null default '{}'::jsonb,
    is_enabled                                boolean      not null default false,
    is_test_provider                          boolean      not null default false
);

create table communication_provider_bindings
(
    id                        serial primary key,
    identity_provider_key     uuid         not null references identity_providers (key) on delete cascade,
    communication_provider_id integer      not null references communication_providers (id) on delete cascade,
    name                      varchar(64)  not null,
    description               varchar(255) not null,
    is_enabled                boolean      not null default false,
    position                  integer      not null default 0,
    configuration             jsonb        not null default '{}'::jsonb
);

create index communication_provider_bindings_identity_provider_key_idx
    on communication_provider_bindings (identity_provider_key, is_enabled, position, name, id);

create index communication_provider_bindings_communication_provider_id_idx
    on communication_provider_bindings (communication_provider_id, identity_provider_key, id);

-- Keep the built-in roles aligned with the defaults used for other global providers.
update system_roles
set permissions = permissions || array [
    'communication_provider.create',
    'communication_provider.read',
    'communication_provider.update',
    'communication_provider.delete'
    ]::varchar[]
where id in (1, 2)
  and not permissions @> array [
    'communication_provider.create',
    'communication_provider.read',
    'communication_provider.update',
    'communication_provider.delete'
    ]::varchar[];

update system_roles
set permissions = permissions || array ['communication_provider.read']::varchar[]
where id = 3
  and not permissions @> array ['communication_provider.read']::varchar[];
