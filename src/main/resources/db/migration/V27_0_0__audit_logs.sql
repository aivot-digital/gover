create table audit_logs
(
    id               bigserial    not null,
    timestamp        timestamp    not null default now(),
    actor_type       varchar(32)  not null,
    actor_id         varchar(255) null,
    trigger_type     varchar(64)  not null,
    trigger_ref      varchar(255) null,
    trigger_ref_type varchar(64)  null,
    module           varchar(128) not null,
    diff             jsonb        null,
    metadata         jsonb        not null default '{}',
    ipaddress        varchar(64)  null,

    primary key (id)
);

create index idx_audit_logs_timestamp on audit_logs (timestamp desc);
create index idx_audit_logs_actor_ts on audit_logs (actor_type, timestamp desc);
create index idx_audit_logs_trigger_ts on audit_logs (trigger_type, timestamp desc);
create index idx_audit_logs_trigger_ref_ts on audit_logs (trigger_ref, timestamp desc);
create index idx_audit_logs_module_ts on audit_logs (module, timestamp desc);
