create table search_recent_items
(
    id            bigserial primary key,
    user_id       varchar(36)  not null references users (id) on delete cascade,
    origin_table  varchar(64)  not null,
    item_id       text         not null,
    created       timestamptz  not null default current_timestamp,
    last_accessed timestamptz  not null default current_timestamp,
    unique (user_id, origin_table, item_id)
);

create index search_recent_items_user_last_accessed_idx
    on search_recent_items (user_id, last_accessed desc);
