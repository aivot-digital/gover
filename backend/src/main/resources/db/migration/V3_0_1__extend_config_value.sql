-- extends value column in config tables

alter table system_configs
    alter column value type VARCHAR;

alter table user_configs
    alter column value type VARCHAR;
