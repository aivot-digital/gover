create table system_configs
(
    key varchar(64) not null
        primary key,
    value varchar not null,
    public_config boolean default false not null
);

create table custom_links
(
    id serial
        constraint provider_links_pkey
            primary key,
    label varchar(128) not null,
    url varchar(500) not null,
    created timestamp with time zone not null,
    updated timestamp with time zone not null,
    description varchar(255),
    icon varchar(64),
    type smallint not null,
    position integer default 0 not null,
    enabled boolean default true not null
);

create index idx_custom_links_type_position
    on custom_links (type, enabled, position, id);

create table destinations
(
    id serial
        primary key,
    name varchar(96) not null,
    type smallint not null,
    created timestamp with time zone not null,
    updated timestamp with time zone not null,
    mail_to varchar(255),
    mailcc varchar(255),
    mailbcc varchar(255),
    api_address varchar(255),
    authorization_header varchar(255),
    max_attachment_mega_bytes integer,
    script text
);

create index idx_destinations_name_full_text
    on destinations using gin (to_tsvector('german'::regconfig, name::text));

create table presets
(
    created timestamp with time zone not null,
    updated timestamp with time zone not null,
    title varchar(190) default 'Ubenannte Vorlage'::character varying not null,
    key uuid default gen_random_uuid() not null
        primary key,
    published_version smallint,
    drafted_version smallint,
    version_count integer default 0 not null
);

create table preset_versions
(
    preset_key uuid not null
        constraint preset_versions_preset_fkey
            references presets
            on delete cascade,
    root_element jsonb default '{}'::jsonb not null,
    created timestamp with time zone not null,
    updated timestamp with time zone not null,
    version smallint default 1 not null,
    status smallint default 0,
    published timestamp with time zone,
    revoked timestamp with time zone,
    primary key (preset_key, version)
);

create trigger on_preset_version_insert
    after insert
    on preset_versions
    for each row
    execute procedure handle_preset_version_insert_or_update();

create trigger on_preset_version_update
    after update
    on preset_versions
    for each row
    when (old.status IS DISTINCT FROM new.status)
	execute procedure handle_preset_version_insert_or_update();

create trigger new_preset_version
    after insert
    on preset_versions
    for each row
    execute procedure handle_new_preset_version();

create trigger on_preset_version_update_or_insert
    after insert or update
                        on preset_versions
                        for each row
                        execute procedure handle_preset_version_update_or_insert_to_set_update_timestamp();

create table secrets
(
    key uuid not null
        primary key,
    name varchar(64) not null,
    description varchar(255) not null,
    value text not null,
    salt varchar(24) not null
);

create index idx_secrets_name_full_text
    on secrets using gin (to_tsvector('german'::regconfig, name::text));

create table payment_providers
(
    key uuid not null
        primary key,
    payment_provider_definition_key varchar(32) not null,
    name varchar(64) not null,
    description varchar(255) not null,
    config jsonb not null,
    is_test_provider boolean default false not null,
    is_enabled boolean default false not null,
    payment_provider_definition_version integer default 1 not null
);

create index idx_payment_providers_name_full_text
    on payment_providers using gin (to_tsvector('german'::regconfig, name::text));

create table payment_transactions
(
    key varchar(128) not null
        primary key,
    payment_provider_key uuid not null
        references payment_providers
            on delete cascade,
    payment_request jsonb not null,
    payment_information jsonb,
    payment_error text,
    redirect_url varchar(255) not null,
    created timestamp with time zone not null,
    updated timestamp with time zone not null
);

create table data_object_schemas
(
    key varchar(64) not null
        primary key,
    name varchar(96) not null,
    description text not null,
    id_gen varchar(64) not null,
    schema jsonb not null,
    created timestamp with time zone default CURRENT_TIMESTAMP not null,
    updated timestamp with time zone default CURRENT_TIMESTAMP not null,
    display_fields varchar(64) [] default (ARRAY[]::character varying[])::character varying(64)[]
);

create index idx_data_object_schemas_name_full_text
    on data_object_schemas using gin (to_tsvector('german'::regconfig, name::text));

create table data_object_items
(
    schema_key varchar(64) not null
        references data_object_schemas
            on update cascade on delete cascade,
    id varchar(64) not null,
    data jsonb not null,
    created timestamp with time zone default CURRENT_TIMESTAMP not null,
    updated timestamp with time zone default CURRENT_TIMESTAMP not null,
    deleted timestamp with time zone,
    primary key (schema_key, id)
);

create index idx_data_object_items_id_full_text
    on data_object_items using gin (to_tsvector('german'::regconfig, id::text));

create index idx_data_object_items_data_full_text
    on data_object_items using gin (to_tsvector('german'::regconfig, data::text));

create table teams
(
    id serial
        primary key,
    name varchar(64) not null
        unique,
    created timestamp with time zone default CURRENT_TIMESTAMP not null,
    updated timestamp with time zone default CURRENT_TIMESTAMP not null
);

create table system_roles
(
    id serial
        primary key,
    name varchar(64) not null
        unique,
    description text,
    permissions varchar(64) [] default '{}'::character varying[] not null,
    created timestamp with time zone default CURRENT_TIMESTAMP not null,
    updated timestamp with time zone default CURRENT_TIMESTAMP not null
);

create table users
(
    id varchar(36) not null
        primary key,
    email varchar(255) default ''::character varying not null,
    first_name varchar(255) default ''::character varying not null,
    last_name varchar(255) default ''::character varying not null,
    enabled boolean default false not null,
    verified boolean default false not null,
    deleted_in_idp boolean default false not null,
    system_role_id integer
        references system_roles
                       on delete set null,
    full_name text generated always as ((((first_name)::text || ' '::text) || (last_name)::text)) stored
);

create table user_configs
(
    user_id varchar(36) not null
        references users
            on delete cascade,
    key varchar(64) not null,
    value varchar not null,
    public_config boolean not null,
    primary key (user_id, key)
);

create table team_memberships
(
    id serial
        primary key,
    team_id integer not null
        references teams
            on delete cascade,
    user_id varchar(36) not null
        references users
            on delete cascade,
    created timestamp with time zone default CURRENT_TIMESTAMP not null,
    updated timestamp with time zone default CURRENT_TIMESTAMP not null,
    unique (team_id, user_id)
);

create table user_deputies
(
    id serial
        primary key,
    original_user_id varchar(36) not null
        references users,
    deputy_user_id varchar(36) not null
        references users,
    from_date date not null,
    until_date date,
    constraint user_deputies_check
        check ((original_user_id)::text <> (deputy_user_id)::text),
	constraint user_deputies_check1
		check ((until_date IS NULL) OR (from_date <= until_date))
);

create trigger trg_check_deputy_circular_reference
    before insert
    on user_deputies
    for each row
    execute procedure fun_check_deputy_circular_reference();

create table domain_roles
(
    id serial
        primary key,
    name varchar(64) not null
        unique,
    description text,
    permissions varchar(64) [] default '{}'::character varying[] not null,
    created timestamp with time zone default CURRENT_TIMESTAMP not null,
    updated timestamp with time zone default CURRENT_TIMESTAMP not null
);

create table storage_providers
(
    id serial
        primary key,
    storage_provider_definition_key varchar(255) not null,
    storage_provider_definition_version integer not null,
    name varchar(255) not null,
    description text not null,
    type smallint not null,
    status smallint default 0 not null,
    status_message text,
    read_only_storage boolean default false not null,
    configuration jsonb default '{}'::jsonb not null,
    max_file_size_in_bytes bigint default 0 not null,
    system_provider boolean default false not null,
    test_provider boolean default false not null,
    metadata_attributes jsonb default '[]'::jsonb not null,
    last_sync timestamp with time zone,
    created timestamp with time zone default now() not null,
    updated timestamp with time zone default now() not null,
    sync_run_id uuid,
    sync_started timestamp with time zone
);

create table storage_index_items
(
    storage_provider_id integer not null
        references storage_providers
            on delete cascade,
    storage_provider_type smallint not null,
    path_from_root text not null,
    directory boolean not null,
    filename varchar(255) not null,
    mime_type varchar(255),
    size_in_bytes bigint default 0 not null,
    missing boolean default false not null,
    metadata jsonb not null,
    created timestamp with time zone default now() not null,
    updated timestamp with time zone default now() not null,
    primary key (storage_provider_id, path_from_root)
);

create table assets
(
    key uuid not null
        primary key,
    uploader_id varchar(64)
        references users
            on delete cascade,
    is_private boolean default true not null,
    storage_provider_id integer not null,
    storage_path_from_root text not null,
    unique (storage_provider_id, storage_path_from_root),
    foreign key (storage_provider_id, storage_path_from_root) references storage_index_items
        on update cascade on delete cascade
);

create table themes
(
    id serial
        primary key,
    name varchar(96) not null,
    logo_key uuid
                     references assets
                         on delete set null,
    favicon_key uuid
                     references assets
                         on delete set null,
    primary_color varchar(7) not null,
    secondary_color varchar(7) not null,
    primary_color_dark varchar(7),
    secondary_color_dark varchar(7),
    logo_key_dark uuid
        references assets
                         on delete set null
);

create table departments
(
    id serial
        primary key,
    name varchar(96) not null,
    imprint text,
    common_privacy text,
    common_accessibility text,
    created timestamp with time zone not null,
    updated timestamp with time zone not null,
    theme_id integer
        references themes
                         on delete set null,
    postal_address text,
    technical_support_email varchar(255),
    technical_support_phone varchar(96),
    technical_support_info text,
    special_support_email varchar(255),
    special_support_phone varchar(96),
    special_support_info text,
    default_mail_signature text,
    depth integer default 0 not null,
    parent_department_id integer
        references departments
                         on delete set null
);

create index idx_departments_name_full_text
    on departments using gin (to_tsvector('german'::regconfig, name::text));

create trigger trg_on_org_unit_insert_or_update
    before insert or update
                         of parent_department_id
                     on departments
                         for each row
                         execute procedure fun_count_depth_on_org_unit_insert_or_update();

create trigger trg_recalculate_child_depths
    after update
    on departments
    for each row
    when (old.parent_department_id IS DISTINCT FROM new.parent_department_id)
	execute procedure fun_recalculate_org_unit_child_depths();

create table department_memberships
(
    id serial
        primary key,
    department_id integer not null
        references departments
            on delete cascade,
    user_id varchar(255) not null
        references users
            on delete cascade,
    created timestamp with time zone default CURRENT_TIMESTAMP not null,
    updated timestamp with time zone default CURRENT_TIMESTAMP not null,
    unique (department_id, user_id)
);

create index idx_themes_name_full_text
    on themes using gin (to_tsvector('german'::regconfig, name::text));

create table identity_providers
(
    key uuid not null
        primary key,
    metadata_identifier varchar(64) not null,
    type smallint default 0 not null,
    name varchar(64) not null,
    description varchar(255) not null,
    icon_asset_key uuid
        references assets
            on delete restrict,
    authorization_endpoint varchar(255) not null,
    token_endpoint varchar(255) not null,
    userinfo_endpoint varchar(255),
    end_session_endpoint varchar(255),
    client_id varchar(128) not null,
    client_secret_key uuid
        references secrets
            on delete restrict,
    attributes jsonb not null,
    default_scopes jsonb not null,
    additional_params jsonb not null,
    is_enabled boolean default false not null,
    is_test_provider boolean default true not null,
    pkce_method varchar(24),
    unique_id_attribute varchar(255) not null
);

create index idx_identity_providers_name_full_text
    on identity_providers using gin (to_tsvector('german'::regconfig, name::text));

create table domain_role_assignments
(
    id serial
        primary key,
    department_membership_id integer
        references department_memberships
            on delete cascade,
    team_membership_id integer
        references team_memberships
            on delete cascade,
    domain_role_id integer not null
        references domain_roles
            on delete cascade,
    created timestamp with time zone default CURRENT_TIMESTAMP not null,
    constraint domain_role_assignments_department_membership_id_team_membe_key
        unique nulls not distinct (department_membership_id, team_membership_id, domain_role_id),
    constraint domain_role_assignments_check
        check ((department_membership_id IS NULL) <> (team_membership_id IS NULL))
);

create table processes
(
    id serial
        primary key,
    internal_title varchar(96) not null,
    department_id integer not null
        references departments
            on delete cascade,
    access_key uuid not null
        unique,
    version_count integer default 0 not null,
    drafted_version integer,
    published_version integer,
    created timestamp with time zone default now() not null,
    updated timestamp with time zone default now() not null,
    slug varchar(128) not null
        constraint processes_slug_unique
            unique
);

create index idx_processes_dashboard_updated
    on processes (updated desc);

create table process_versions
(
    process_id integer not null
        references processes
            on delete cascade,
    process_version integer default 0 not null,
    status smallint default 0 not null,
    public_title varchar(192) not null,
    case_number_type varchar(32) default 'CROCKFORD_BASE32'::character varying not null
		constraint process_versions_case_number_type_check
			check ((case_number_type)::text = ANY ((ARRAY['CROCKFORD_BASE32'::character varying, 'UUID_V4'::character varying, 'UUID_V7'::character varying, 'TEMPLATE'::character varying])::text[])),
    case_number_template varchar(96),
    notes text,
    theme_id integer
        references themes
                           on delete set null,
    legal_support_department_id integer
        references departments
                           on delete set null,
    technical_support_department_id integer
        references departments
                           on delete set null,
    imprint_department_id integer
        references departments
                           on delete set null,
    privacy_department_id integer
        references departments
                           on delete set null,
    accessibility_department_id integer
        references departments
                           on delete set null,
    process_specific_privacy_statement text,
    process_specific_accessibility_statement text,
    created timestamp with time zone default now() not null,
    updated timestamp with time zone default now() not null,
    published timestamp with time zone,
    revoked timestamp with time zone,
    retention_time_value integer,
    retention_time_unit varchar(16),
    instance_deadline_days integer
        constraint process_versions_instance_deadline_days_check
            check ((instance_deadline_days IS NULL) OR ((instance_deadline_days >= 1) AND (instance_deadline_days <= 3652))),
    primary key (process_id, process_version),
    constraint process_versions_case_number_template_check
        check (((case_number_type)::text = 'TEMPLATE'::text) = (case_number_template IS NOT NULL)),
	constraint process_versions_retention_time_pair_check
		check (((retention_time_value IS NULL) AND (retention_time_unit IS NULL)) OR ((retention_time_value IS NOT NULL) AND (retention_time_value > 0) AND (retention_time_unit IS NOT NULL) AND ((retention_time_unit)::text = ANY ((ARRAY['Days'::character varying, 'Weeks'::character varying, 'Months'::character varying, 'Years'::character varying])::text[]))))
);

create trigger on_process_version_insert
    after insert
    on process_versions
    for each row
    execute procedure handle_process_version_insert();

create trigger on_process_version_update
    before update
    on process_versions
    for each row
    execute procedure handle_process_version_update();

create trigger on_process_version_delete
    before delete
    on process_versions
    for each row
    execute procedure handle_process_version_delete();

create table process_nodes
(
    id serial
        primary key,
    process_id integer not null
        references processes
            on delete cascade,
    process_version integer not null,
    name varchar(96),
    description text,
    data_key varchar(32) not null,
    process_node_definition_key varchar(128) not null,
    process_node_definition_version integer not null,
    configuration jsonb default '{}'::jsonb not null,
    output_mappings jsonb default '{}'::jsonb not null,
    deadlines jsonb default '{}'::jsonb not null,
    requirements text,
    notes text,
    saved_with_errors boolean default false not null,
    created timestamp with time zone default now() not null,
    updated timestamp with time zone default now() not null,
    unique (process_id, process_version, data_key),
    foreign key (process_id, process_version) references process_versions
        on delete cascade
);

create trigger update_parent_process_entities_timestamps_trigger
    after insert or update or delete
                    on process_nodes
                        for each row
                        execute procedure update_parent_process_entities_timestamps();

create table process_edges
(
    id serial
        primary key,
    process_id integer not null
        references processes
            on delete cascade,
    process_version integer not null,
    from_node_id integer not null
        references process_nodes
            on delete cascade,
    to_node_id integer not null
        references process_nodes
            on delete cascade,
    via_port varchar(32) not null,
    unique (from_node_id, via_port),
    foreign key (process_id, process_version) references process_versions
        on delete cascade
);

create trigger update_parent_process_entities_timestamps_trigger
    after insert or update or delete
                    on process_edges
                        for each row
                        execute procedure update_parent_process_entities_timestamps();

create table process_test_configs
(
    id serial
        primary key,
    name varchar(128) not null,
    process_id integer not null,
    process_version integer not null,
    configs jsonb not null,
    created timestamp with time zone default now() not null,
    updated timestamp with time zone default now() not null,
    foreign key (process_id, process_version) references process_versions
        on delete cascade
);

create table process_test_claims
(
    id serial
        primary key,
    access_key varchar(64) not null
        unique,
    process_test_config_id integer
        references process_test_configs
            on delete cascade,
    process_id integer not null,
    process_version integer not null,
    created timestamp with time zone default now() not null,
    owning_user_id varchar(36) not null
        references users
            on delete restrict,
    unique (process_id, process_version),
    foreign key (process_id, process_version) references process_versions
        on delete cascade
);

create table process_instances
(
    id bigserial
        primary key,
    case_number varchar(36) not null
        unique,
    access_key varchar(128) not null
        unique,
    process_id integer not null
        references processes
            on delete cascade,
    initial_process_version integer not null,
    status smallint default 0 not null,
    status_override varchar(96),
    assigned_user_id varchar(36)
        references users
            on delete restrict,
    assigned_file_numbers varchar(96) [] default '{}'::character varying[] not null,
    identities jsonb default '{}'::jsonb not null,
    started timestamp with time zone default now() not null,
    updated timestamp with time zone default now() not null,
    finished timestamp with time zone,
    runtime interval generated always as ((finished - started)) stored,
    initial_payload jsonb default '{}'::jsonb not null,
    initial_node_id integer not null
        references process_nodes
            on delete restrict,
    keep_until timestamp with time zone,
    created_for_test_claim_id integer
        references process_test_claims
            on delete cascade,
    inbound_reference varchar(128),
    deadline timestamp with time zone
);

create index process_instances_compact_case_number_search_idx
    on process_instances using gin (compact_case_number_search_key(case_number::text) gin_trgm_ops);

create index idx_process_instances_dashboard_started
    on process_instances (started)
    where (created_for_test_claim_id IS NULL);

create index idx_process_instances_dashboard_finished
    on process_instances (finished)
    where ((created_for_test_claim_id IS NULL) AND (finished IS NOT NULL));

create index idx_process_instances_dashboard_status
    on process_instances (status)
    where (created_for_test_claim_id IS NULL);

create unique index process_instances_inbound_reference_unique
    on process_instances (inbound_reference)
    where (inbound_reference IS NOT NULL);

create index process_instances_retention_cleanup_idx
    on process_instances (status, keep_until, id);

create index idx_process_instances_dashboard_assignment
    on process_instances (assigned_user_id, status, deadline, started, id)
    where (assigned_user_id IS NOT NULL);

create trigger on_process_instance_insert
    after insert
    on process_instances
    for each row
    execute procedure handle_process_instance_insert();

create table process_instance_tasks
(
    id bigserial
        primary key,
    access_key varchar(128) not null,
    process_instance_id bigint not null
        references process_instances
            on delete cascade,
    process_id integer not null
        references processes
            on delete cascade,
    process_version integer not null,
    process_node_id integer not null
        references process_nodes
            on delete restrict,
    previous_process_instance_task_id bigint,
    previous_process_node_id integer,
    previous_process_node_port_key varchar(96),
    status smallint default 0 not null,
    status_override varchar(96),
    started timestamp with time zone default now() not null,
    updated timestamp with time zone default now() not null,
    finished timestamp with time zone,
    runtime interval generated always as ((finished - started)) stored,
    runtime_data jsonb default '{}'::jsonb not null,
    node_data jsonb default '{}'::jsonb not null,
    process_data jsonb default '{}'::jsonb not null,
    assigned_user_id varchar(36)
        references users
            on delete restrict,
    assigned_customer_identity_id varchar(36),
    deadline timestamp with time zone,
    deadline_key varchar(32),
    deadline_strict boolean default false not null,
    postponed_until timestamp with time zone,
    retry_count integer default 0,
    next_retry_at timestamp with time zone,
    process_data_diff jsonb default '{}'::jsonb,
    execution_summary_markdown text,
    restart_for_task_id bigint
        constraint process_instance_tasks_restart_for_task_fk
            references process_instance_tasks,
    unique (process_instance_id, access_key),
    foreign key (process_id, process_version) references process_versions
        on delete restrict,
    constraint process_instance_tasks_check
        check ((deadline IS NULL) = (deadline_key IS NULL)),
    constraint process_instance_tasks_check1
        check ((deadline_key IS NOT NULL) OR (NOT deadline_strict)),
    constraint process_instance_tasks_restart_not_self
        check (restart_for_task_id <> id)
);

comment on column process_instance_tasks.status is '0 Running, 1 Paused, 2 Completed, 3 Aborted, 4 Failed, 5 Restarted, 6 AwaitingPayment, 7 AwaitingCustomer, 8 InProgress, 9 AwaitingStaff, 10 DeadlineExceeded';

create index idx_process_instance_tasks_dashboard_assignment
    on process_instance_tasks (assigned_user_id, status, deadline, started, id)
    where (assigned_user_id IS NOT NULL);

create index process_instance_tasks_restart_for_task_idx
    on process_instance_tasks (restart_for_task_id);

create index process_instance_tasks_previous_task_idx
    on process_instance_tasks (previous_process_instance_task_id);

create index process_instance_tasks_due_deadline_idx
    on process_instance_tasks (deadline, id)
    where ((status = ANY (ARRAY[0, 6, 7, 8, 9])) AND deadline_strict);

create trigger trg_process_task_completion
    after insert or update
                        of status
                    on process_instance_tasks
                        for each row
                        when (new.status = 2)
                        execute procedure handle_process_task_completion();

create table process_instance_events
(
    id bigserial
        primary key,
    process_instance_id bigint not null
        references process_instances
            on delete cascade,
    process_instance_task_id bigint
        references process_instance_tasks
            on delete cascade,
    level smallint default 0 not null,
    is_technical boolean default false not null,
    is_audit boolean default false not null,
    is_history_relevant boolean default false not null,
    title varchar(96) not null,
    message text default ''::text not null,
    details jsonb default '{}'::jsonb not null,
    timestamp timestamp with time zone default now() not null,
    triggering_user_id varchar(36)
        references users
            on delete restrict,
    concerned_user_id varchar(36),
    concerned_identity_id varchar(255),
    concerned_identity_title varchar(255),
    constraint process_instance_events_check
        check ((concerned_identity_id IS NULL) = (concerned_identity_title IS NULL))
);

create table process_changes
(
    id bigserial
        primary key,
    timestamp timestamp with time zone default now() not null,
    user_id varchar(36) not null
        references users
            on delete restrict,
    process_id integer not null
        references processes
            on delete cascade,
    process_version integer,
    process_node_id integer
        references process_nodes
            on delete cascade,
    process_edge_id integer
        references process_edges
            on delete cascade,
    change_type smallint not null,
    diff jsonb default '{}'::jsonb not null,
    comment varchar(512),
    foreign key (process_id, process_version) references process_versions
        on delete cascade
);

create table process_access_controls
(
    id serial
        primary key,
    source_team_id integer
        references teams
            on delete cascade,
    source_department_id integer
        references departments
            on delete cascade,
    target_process_id integer not null
        references processes
            on delete cascade,
    permissions varchar(64) [] default '{}'::character varying[] not null,
    created timestamp with time zone default CURRENT_TIMESTAMP not null,
    updated timestamp with time zone default CURRENT_TIMESTAMP not null,
    constraint process_access_controls_source_team_id_source_department_id_key
        unique (source_team_id, source_department_id, target_process_id),
    constraint process_access_controls_check
        check ((source_team_id IS NULL) <> (source_department_id IS NULL))
);

create table process_instance_access_control_presets
(
    id serial
        primary key,
    source_team_id integer
        references teams
            on delete cascade,
    source_department_id integer
        constraint process_instance_access_control_prese_source_department_id_fkey
            references departments
            on delete cascade,
    target_process_id integer not null,
    target_process_version integer not null,
    permissions varchar(64) [] default '{}'::character varying[] not null,
    created timestamp with time zone default CURRENT_TIMESTAMP not null,
    updated timestamp with time zone default CURRENT_TIMESTAMP not null,
    constraint process_instance_access_contr_target_process_id_target_pro_fkey
        foreign key (target_process_id, target_process_version) references process_versions
            on delete cascade,
    constraint process_instance_access_control_presets_check
        check ((source_team_id IS NULL) <> (source_department_id IS NULL))
);

create unique index process_instance_access_control_presets_department_unique
    on process_instance_access_control_presets (source_department_id, target_process_id, target_process_version)
    where (source_department_id IS NOT NULL);

create unique index process_instance_access_control_presets_team_unique
    on process_instance_access_control_presets (source_team_id, target_process_id, target_process_version)
    where (source_team_id IS NOT NULL);

create table process_instance_access_controls
(
    id serial
        primary key,
    source_team_id integer
        references teams
            on delete cascade,
    source_department_id integer
        references departments
            on delete cascade,
    target_process_instance_id bigint not null
        constraint process_instance_access_control_target_process_instance_id_fkey
            references process_instances
            on delete cascade,
    permissions varchar(64) [] default '{}'::character varying[] not null,
    created timestamp with time zone default CURRENT_TIMESTAMP not null,
    updated timestamp with time zone default CURRENT_TIMESTAMP not null,
    constraint process_instance_access_contr_source_team_id_source_departm_key
        unique (source_team_id, source_department_id, target_process_instance_id),
    constraint process_instance_access_controls_check
        check ((source_team_id IS NULL) <> (source_department_id IS NULL))
);

create table process_instance_attachment_sets
(
    id serial
        primary key,
    name varchar(255) not null,
    data_key varchar(255) not null,
    process_instance_id bigint not null
        references process_instances
            on delete restrict,
    process_instance_task_id bigint
        references process_instance_tasks
            on delete restrict
);

create table process_instance_attachments
(
    key uuid not null
        primary key,
    file_name varchar(255) not null,
    original_file_name varchar(255) not null,
    "group" text,
    attachment_set_id integer not null
        references process_instance_attachment_sets
            on delete restrict,
    process_instance_id bigint not null
        references process_instances
            on delete restrict,
    process_instance_task_id bigint
        references process_instance_tasks
            on delete restrict,
    storage_provider_id integer not null,
    storage_path_from_root text not null,
    uploaded_by_user_id varchar(36)
        references users
            on delete restrict,
    position integer default 1 not null,
    constraint process_instance_attachments_storage_provider_id_storage_p_fkey
        foreign key (storage_provider_id, storage_path_from_root) references storage_index_items
);

create table audit_logs
(
    id bigserial
        primary key,
    timestamp timestamp with time zone default now() not null,
    actor_type varchar(32) not null,
    actor_id varchar(255),
    origin text not null,
    trigger_type varchar(64) not null,
    entity_type varchar(128),
    entity_ref varchar(255),
    entity_ref_type varchar(64),
    module varchar(128) not null,
    message text default ''::text not null,
    diff jsonb,
    metadata jsonb default '{}'::jsonb not null,
    ipaddress varchar(64)
);

create table process_slug_history
(
    slug varchar(128) not null
        primary key,
    process_id integer not null
        references processes
            on delete cascade,
    created timestamp with time zone default now() not null
);

create index process_slug_history_process_id_idx
    on process_slug_history (process_id);

create table code_lists
(
    key varchar(255) not null
        primary key,
    id serial
        unique,
    source_type integer default 0 not null,
    source_ref varchar(96) default ''::character varying not null,
    name varchar(96) not null,
    description text not null,
    columns text[] not null,
    value_column_index integer default 0 not null,
    label_column_index integer default 0 not null,
    status smallint default 0 not null,
    status_message text,
    last_sync timestamp with time zone,
    created timestamp with time zone default CURRENT_TIMESTAMP not null,
    updated timestamp with time zone default CURRENT_TIMESTAMP not null,
    sync_keep_outdated boolean default false not null,
    sync_run_id uuid,
    sync_started timestamp with time zone
);

create table code_list_items
(
    id bigserial
        primary key,
    code_list_id integer not null
        references code_lists (id)
            on delete cascade,
    columns text[] not null,
    created timestamp with time zone default CURRENT_TIMESTAMP not null,
    updated timestamp with time zone default CURRENT_TIMESTAMP not null
);

create table search_recent_items
(
    id bigserial
        primary key,
    user_id varchar(36) not null
        references users
            on delete cascade,
    origin_table varchar(64) not null,
    item_id text not null,
    created timestamp with time zone default CURRENT_TIMESTAMP not null,
    last_accessed timestamp with time zone default CURRENT_TIMESTAMP not null,
    unique (user_id, origin_table, item_id)
);

create index search_recent_items_user_last_accessed_idx
    on search_recent_items (user_id asc, last_accessed desc);

create table communication_providers
(
    id serial
        primary key,
    communication_provider_definition_key varchar(255) not null,
    communication_provider_definition_version integer not null,
    name varchar(64) not null,
    description varchar(255) not null,
    configuration jsonb default '{}'::jsonb not null,
    is_enabled boolean default false not null,
    is_test_provider boolean default false not null
);

create table communication_provider_bindings
(
    id serial
        primary key,
    identity_provider_key uuid not null
        references identity_providers
            on delete cascade,
    communication_provider_id integer not null
        references communication_providers
            on delete cascade,
    name varchar(64) not null,
    description varchar(255) not null,
    is_enabled boolean default false not null,
    position integer default 0 not null,
    configuration jsonb default '{}'::jsonb not null
);

create index communication_provider_bindings_identity_provider_key_idx
    on communication_provider_bindings (identity_provider_key, is_enabled, position, name, id);

create index communication_provider_bindings_communication_provider_id_idx
    on communication_provider_bindings (communication_provider_id, identity_provider_key, id);

create table process_work_requests
(
    id bigserial
        primary key,
    kind smallint not null
        constraint process_work_requests_kind_check
            check (kind = ANY (ARRAY[0, 1, 2, 3])),
    process_instance_id bigint not null
        references process_instances
            on delete cascade,
    task_id bigint
        references process_instance_tasks
            on delete cascade,
    via_port varchar(96),
    to_node_id integer,
    created timestamp with time zone not null,
    last_dispatched timestamp with time zone,
    constraint process_work_requests_task_check
        check ((kind = 0) = (task_id IS NULL)),
    constraint process_work_requests_port_check
        check ((kind = 1) = (via_port IS NOT NULL)),
    constraint process_work_requests_node_check
        check ((kind = 1) = (to_node_id IS NOT NULL))
);

create unique index process_work_requests_start_idx
    on process_work_requests (process_instance_id)
    where (kind = 0);

create unique index process_work_requests_task_idx
    on process_work_requests (task_id, kind)
    where (kind = ANY (ARRAY[1, 2]));

create index process_work_requests_instance_idx
    on process_work_requests (process_instance_id);

