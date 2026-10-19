create table ai_chat_sessions
(
    id         bigserial primary key,
    user_id    varchar(36) not null references users (id) on delete cascade,
    session_id varchar(32) not null,
    created    timestamptz not null default current_timestamp,
    updated    timestamptz not null default current_timestamp,
    messages   jsonb       not null default '[]',
    chat_messages jsonb    not null default '[]',
    version    bigint      not null default 0,

    constraint ai_chat_sessions_user_session_key unique (user_id, session_id)
);

create index ai_chat_sessions_user_updated_idx
    on ai_chat_sessions (user_id, updated desc, id desc);
