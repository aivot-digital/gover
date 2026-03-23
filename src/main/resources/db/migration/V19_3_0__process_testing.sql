-- Add a table for storing process test configurations
-- These should be used when running tests on specific process versions
create table process_test_configs
(
    id              serial primary key,
    name            varchar(128) not null,
    process_id      int          not null,
    process_version int          not null,
    configs         jsonb        not null,
    created         timestamp    not null default now(),
    updated         timestamp    not null default now(),
    foreign key (process_id, process_version) references process_versions (process_id, process_version) on delete cascade
);

-- Claims by users to run tests on specific process versions
-- These claims allow users to have exclusive access to run tests on a process version
create table process_test_claims
(
    id                     serial primary key,
    access_key             varchar(64) not null unique,
    process_test_config_id int         null,
    process_id             int         not null,
    process_version        int         not null,
    created                timestamp   not null default now(),
    owning_user_id         varchar(36) not null,
    foreign key (process_test_config_id) references process_test_configs (id) on delete cascade,
    foreign key (owning_user_id) references users (id) on delete restrict,
    foreign key (process_id, process_version) references process_versions (process_id, process_version) on delete cascade,
    unique (process_id, process_version) -- There can be only one active claim per process version
);

-- Link process instances to test claims
alter table process_instances
    add column created_for_test_claim_id int null references process_test_claims (id) on delete cascade;
