create table queries
(
    key               uuid primary key,
    title             varchar(255) not null,
    description       text,
    query             jsonb        not null,
    type              smallint     not null default 0,
    show_on_dashboard boolean      not null default false,
    is_system_query   boolean      not null default false
);