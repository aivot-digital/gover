create table audit_logs
(
    id                 bigserial     not null,
    event_ts           timestamp     not null default now(),
    triggering_user_id varchar(36)   null,
    actor_type         varchar(32)   not null default 'SYSTEM',
    actor_id           varchar(255)  null,
    actor_label        varchar(255)  null,
    trigger_type       varchar(32)   null,
    trigger_ref        varchar(255)  null,
    service_name       varchar(128)  null,
    instance_id        varchar(128)  null,
    action_type        varchar(64)   not null,
    component          varchar(255)  not null,
    entity_type        varchar(255)  null,
    entity_id          varchar(255)  null,
    message            text          not null default '',

    changed_data       boolean       not null default false,
    before_data        jsonb         null,
    after_data         jsonb         null,
    data_diff          jsonb         null,

    action_result      varchar(32)   not null default 'success',
    reason             varchar(512)  null,
    source             varchar(32)   null,
    request_id         varchar(128)  null,
    session_id         varchar(128)  null,
    ip_address         varchar(64)   null,
    user_agent         varchar(1024) null,
    severity           varchar(32)   null,
    tags               varchar(64)[] not null default '{}',
    metadata           jsonb         not null default '{}',
    created_at         timestamp     not null default now(),

    primary key (id),
    foreign key (triggering_user_id) references users (id) on delete restrict
);

create index idx_audit_logs_event_ts on audit_logs (event_ts desc);
create index idx_audit_logs_actor_ts on audit_logs (actor_type, event_ts desc);
create index idx_audit_logs_user_ts on audit_logs (triggering_user_id, event_ts desc);
create index idx_audit_logs_component_ts on audit_logs (component, event_ts desc);
create index idx_audit_logs_entity_ts on audit_logs (entity_type, entity_id, event_ts desc);
create index idx_audit_logs_request_id on audit_logs (request_id);
create index idx_audit_logs_action_type on audit_logs (action_type);
