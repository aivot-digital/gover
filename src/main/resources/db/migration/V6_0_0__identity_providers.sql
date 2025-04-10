-- transform type of client secret key to uuid
alter table secrets
    alter column key type uuid using key::uuid;

-- create table for identity providers
create table identity_providers
(
    key                    uuid         not null,
    metadata_identifier    varchar(64)  not null,
    type                   smallint     not null default 0,
    name                   varchar(64)  not null,
    description            varchar(255) not null,
    icon_asset_key         uuid         null,
    authorization_endpoint varchar(255) not null,
    token_endpoint         varchar(255) not null,
    userinfo_endpoint      varchar(255) not null,
    end_session_endpoint   varchar(255) not null,
    client_id              varchar(32)  not null,
    client_secret_key      uuid         null,
    attributes             jsonb        not null,
    default_scopes         jsonb        not null,
    additional_params      jsonb        not null,
    is_enabled             boolean      not null default false,
    is_test_provider       boolean      not null default true,
    primary key (key),
    foreign key (icon_asset_key) references assets (key) on delete restrict,
    foreign key (client_secret_key) references secrets (key) on delete restrict
);

-- create column for identity provider key in user table
alter table forms
    add column identity_required  boolean not null default false,
    add column identity_providers jsonb default '[]';

update forms
set identity_required = true
where (bund_id_enabled and bund_id_level > 0)
   or (bayern_id_enabled and bayern_id_level > 0)
   or (sh_id_enabled and sh_id_level > 0)
   or (muk_enabled and muk_level > 0);