-- create table for payment providers
create table payment_providers
(
    key          VARCHAR(36) primary key,
    provider_key VARCHAR(32)  NOT NULL,
    name         VARCHAR(64)  NOT NULL,
    description  VARCHAR(255) NOT NULL,
    config       jsonb        NOT NULL
);