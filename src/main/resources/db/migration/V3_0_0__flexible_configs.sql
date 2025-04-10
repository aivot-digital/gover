-- remove unnecessary columns from system_configs table

alter table system_configs
    drop column updated,
    drop column created;

-- create user_configs table

create table user_configs
(
    user_id       VARCHAR(36) NOT NULL,
    key           VARCHAR(64) NOT NULL,
    value         VARCHAR(96) NOT NULL,
    public_config BOOLEAN     NOT NULL,
    PRIMARY KEY (user_id, key)
);