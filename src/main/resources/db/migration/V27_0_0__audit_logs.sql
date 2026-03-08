create table audit_logs
(
    id              bigserial    not null,
    timestamp       timestamp    not null default now(),
    actor_type      varchar(32)  not null,
    actor_id        varchar(255) null,
    trigger_type    varchar(64)  not null,
    entity_type     varchar(128) not null,
    entity_ref      varchar(255) null,
    entity_ref_type varchar(64)  null,
    module          varchar(128) not null,
    message         text         not null default '',
    diff            jsonb        null,
    metadata        jsonb        not null default '{}',
    ipaddress       varchar(64)  null,

    primary key (id)
);
