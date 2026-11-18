create table user_api_keys
(
    id         serial
        primary key,
    user_id    varchar(36)              not null
        references users on delete cascade,
    -- User-chosen label that identifies the integration using the key
    name       varchar(255)             not null,
    created    timestamp with time zone default now() not null,
    expiration timestamp with time zone not null,
    -- Lowercase hex SHA-256 digest of the complete key including its prefix; the key itself is never stored
    key_hash   varchar(64)              not null
        unique
);

-- Lets deleting a user cascade to its keys without scanning the whole table
create index user_api_keys_user_id_idx
    on user_api_keys (user_id);
