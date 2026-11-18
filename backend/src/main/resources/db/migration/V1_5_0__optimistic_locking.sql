-- Optimistic locking: Hibernate increments lock_version on every update of a row through JPA.
-- Existing rows start at 0, so example datasets and inserts without the column keep working.
alter table teams
    add column if not exists lock_version bigint not null default 0;

alter table departments
    add column if not exists lock_version bigint not null default 0;

alter table users
    add column if not exists lock_version bigint not null default 0;

alter table user_deputies
    add column if not exists lock_version bigint not null default 0;

alter table system_roles
    add column if not exists lock_version bigint not null default 0;

alter table domain_roles
    add column if not exists lock_version bigint not null default 0;

alter table identity_providers
    add column if not exists lock_version bigint not null default 0;

alter table payment_providers
    add column if not exists lock_version bigint not null default 0;

alter table communication_providers
    add column if not exists lock_version bigint not null default 0;

alter table communication_provider_bindings
    add column if not exists lock_version bigint not null default 0;

alter table storage_providers
    add column if not exists lock_version bigint not null default 0;

alter table secrets
    add column if not exists lock_version bigint not null default 0;

alter table themes
    add column if not exists lock_version bigint not null default 0;

alter table custom_links
    add column if not exists lock_version bigint not null default 0;

alter table code_lists
    add column if not exists lock_version bigint not null default 0;

alter table code_list_items
    add column if not exists lock_version bigint not null default 0;

alter table data_object_schemas
    add column if not exists lock_version bigint not null default 0;

alter table data_object_items
    add column if not exists lock_version bigint not null default 0;

alter table presets
    add column if not exists lock_version bigint not null default 0;

alter table preset_versions
    add column if not exists lock_version bigint not null default 0;

alter table processes
    add column if not exists lock_version bigint not null default 0;

alter table process_versions
    add column if not exists lock_version bigint not null default 0;

alter table process_nodes
    add column if not exists lock_version bigint not null default 0;

alter table process_edges
    add column if not exists lock_version bigint not null default 0;

alter table process_test_configs
    add column if not exists lock_version bigint not null default 0;

alter table process_access_controls
    add column if not exists lock_version bigint not null default 0;

alter table process_instance_access_control_presets
    add column if not exists lock_version bigint not null default 0;

alter table process_instance_access_controls
    add column if not exists lock_version bigint not null default 0;

-- Expose the lock version of code list items, because their update endpoint responds with this view.
create or replace view v_code_list_items(id, code_list_id, columns, created, updated, value, label, lock_version) as
SELECT cli.id,
       cli.code_list_id,
       cli.columns,
       cli.created,
       cli.updated,
       cli.columns[cl.value_column_index + 1] AS value,
       cli.columns[cl.label_column_index + 1] AS label,
       cli.lock_version
FROM code_list_items cli
         JOIN code_lists cl ON cli.code_list_id = cl.id;
