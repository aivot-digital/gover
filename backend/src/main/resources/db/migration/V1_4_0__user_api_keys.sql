create table user_api_keys
(
    id         serial
        primary key,
    user_id    varchar(36)              not null
        references users on delete cascade,
    expiration timestamp with time zone not null,
    -- Lowercase hex SHA-256 digest of the complete key including its prefix; the key itself is never stored
    key_hash   varchar(64)              not null
        unique
);
