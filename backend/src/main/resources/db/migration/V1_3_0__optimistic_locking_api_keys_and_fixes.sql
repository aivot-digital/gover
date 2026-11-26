-- ===== Process instance attachment removal =====

-- Task views flag the stored attachments of a task that its upload fields no longer reference. The flagged attachments
-- are hidden and are due for deletion when the task ends.
alter table process_instance_attachments
    add column removed_at timestamp with time zone;

-- Due attachments keep their row until their file is deleted from storage, so a failed deletion is retried.
alter table process_instance_attachments
    add column deletion_due_at timestamp with time zone;

-- Every end of a task looks up the flagged attachments of the task.
create index idx_process_instance_attachments_removed_task
    on process_instance_attachments (process_instance_task_id)
    where removed_at is not null;

-- The retry looks up the attachments whose deletion is overdue.
create index idx_process_instance_attachments_deletion_due
    on process_instance_attachments (deletion_due_at)
    where deletion_due_at is not null;

-- ===== Clarify seeded identity salutations =====

-- Update only unchanged salutation descriptions seeded for BayernID, BundID
-- and Servicekonto Schleswig-Holstein, in both production and test variants.
-- Preserve customized mappings, technical keys and the attribute order.
UPDATE identity_providers AS provider
SET attributes = (
    SELECT jsonb_agg(
                   CASE
                       WHEN attribute @> '{"label":"Anrede","keyInData":"salutation","description":"Formelle Anrede, z. B. Herr, Frau oder Divers."}'::jsonb
                           THEN jsonb_set(
                               attribute,
                               '{description}',
                               to_jsonb('Anrede, wie sie vom Identitätsanbieter übermittelt wird, zum Beispiel Herr oder Frau.'::text),
                               false
                           )
                       ELSE attribute
                       END
                   ORDER BY position
           )
    FROM jsonb_array_elements(provider.attributes) WITH ORDINALITY AS entry(attribute, position)
)
WHERE provider.type IN (1, 2, 3)
  AND jsonb_typeof(provider.attributes) = 'array'
  AND provider.attributes @> '[{"label":"Anrede","keyInData":"salutation","description":"Formelle Anrede, z. B. Herr, Frau oder Divers."}]'::jsonb;

-- ===== Correct default preset title =====

-- Correct the database default without changing existing, editable template titles.
ALTER TABLE presets
    ALTER COLUMN title SET DEFAULT 'Unbenannte Vorlage';

-- ===== User api keys =====

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

-- ===== Optimistic locking =====

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

-- ===== Process instance tasks active status index =====

-- Counts the tasks that are still open for the metrics without scanning the finished tasks.
-- Statuses: Running, Paused, AwaitingPayment, AwaitingCustomer, InProgress, AwaitingStaff.
create index if not exists idx_process_instance_tasks_open_status
    on process_instance_tasks (status)
    where (status = ANY (ARRAY[0, 1, 6, 7, 8, 9]));

-- ===== Process access managing department =====

-- Access rules only add access for their source. A department's rights on the processes it manages come from its roles
-- alone, so a rule that targets another process must no longer restrict them. This matches
-- v_user_process_instance_access_permissions.
create or replace view v_user_process_access_permissions(user_id, via_source_team_id, via_source_department_id, target_process_id, permissions) as
WITH raw_permissions AS (SELECT udp.user_id,
                                NULL::integer     AS via_source_team_id,
                                pro.department_id AS via_source_department_id,
                                pro.id            AS target_process_id,
                                udp.permissions
                         FROM processes pro
                                  JOIN v_user_domain_permissions udp ON udp.department_id = pro.department_id
                         UNION ALL
                         SELECT udp.user_id,
                                pac.source_team_id                                AS via_source_team_id,
                                pac.source_department_id                          AS via_source_department_id,
                                pac.target_process_id,
                                array_intersect(udp.permissions, pac.permissions) AS permissions
                         FROM process_access_controls pac
                                  JOIN v_user_domain_permissions udp ON pac.source_department_id IS NOT NULL AND
                                                                        udp.department_id = pac.source_department_id OR
                                                                        pac.source_team_id IS NOT NULL AND
                                                                        udp.team_id = pac.source_team_id)
SELECT user_id,
       via_source_team_id,
       via_source_department_id,
       target_process_id,
       array_unique_union_agg(permissions) AS permissions
FROM raw_permissions
WHERE array_length(permissions, 1) > 0
GROUP BY user_id, via_source_team_id, via_source_department_id, target_process_id;

-- ===== User deputy cycle detection =====

-- Whether saving the deputy relation original -> deputy for the given period would close a cycle, i.e. whether the
-- original user already represents the deputy, directly or through further deputies, on a day of that period. Only
-- relations whose periods overlap form a cycle: mutual cover for different holidays stays possible. The relation with
-- the ignored ID is left out, so an update does not compare a relation with its own previous version.
create or replace function fun_user_deputy_creates_cycle(p_original_user_id varchar,
                                                         p_deputy_user_id varchar,
                                                         p_from_date date,
                                                         p_until_date date,
                                                         p_ignored_id integer) returns boolean
    language sql
    stable
as
$$
WITH RECURSIVE reachable AS (SELECT ud.deputy_user_id::text                   AS user_id,
                                    greatest(ud.from_date, p_from_date)        AS from_date,
                                    -- least ignores NULL, which matches NULL meaning an unlimited period
                                    least(ud.until_date, p_until_date)         AS until_date,
                                    ARRAY [ud.original_user_id::text, ud.deputy_user_id::text] AS path
                             FROM user_deputies ud
                             WHERE ud.original_user_id::text = p_deputy_user_id::text
                               AND ud.id IS DISTINCT FROM p_ignored_id
                               AND (ud.until_date IS NULL OR p_from_date <= ud.until_date)
                               AND (p_until_date IS NULL OR ud.from_date <= p_until_date)
                             UNION ALL
                             SELECT ud.deputy_user_id::text,
                                    greatest(ud.from_date, r.from_date),
                                    least(ud.until_date, r.until_date),
                                    r.path || ud.deputy_user_id::text
                             FROM user_deputies ud
                                      JOIN reachable r ON ud.original_user_id::text = r.user_id
                             WHERE ud.id IS DISTINCT FROM p_ignored_id
                               AND r.user_id <> p_original_user_id::text
                               AND (ud.until_date IS NULL OR r.from_date <= ud.until_date)
                               AND (r.until_date IS NULL OR ud.from_date <= r.until_date)
                               -- Existing cycles must not make the search endless
                               AND NOT ud.deputy_user_id::text = ANY (r.path))
SELECT EXISTS(SELECT 1 FROM reachable WHERE user_id = p_original_user_id::text);
$$;

-- The previous check only ran on insert and only saw relations active today, so relations starting later or extended
-- by an update could still close a cycle.
create or replace function fun_check_deputy_circular_reference() returns trigger
    language plpgsql
as
$$
begin
    if fun_user_deputy_creates_cycle(NEW.original_user_id, NEW.deputy_user_id, NEW.from_date, NEW.until_date, NEW.id) then
        raise exception 'Circular deputy assignment detected between users % and %', NEW.original_user_id, NEW.deputy_user_id;
    end if;
    return NEW;
end;
$$;

drop trigger if exists trg_check_deputy_circular_reference on user_deputies;

create trigger trg_check_deputy_circular_reference
    before insert or update
    on user_deputies
    for each row
execute procedure fun_check_deputy_circular_reference();

-- Without a guard, an active cycle made this recursion endless and with it every permission check built on it. The
-- trigger prevents new cycles, but cycles that already exist or that concurrent saves create must stay harmless.
create or replace view v_user_is_recursively_deputy_for(deputy_user_id, original_user_id, depth) as
WITH RECURSIVE deputy_hierarchy AS (SELECT ud.original_user_id,
                                           ud.deputy_user_id,
                                           1                                                         AS depth,
                                           ARRAY [ud.original_user_id::text, ud.deputy_user_id::text] AS path
                                    FROM user_deputies ud
                                    WHERE ud.from_date <= CURRENT_DATE
                                      AND (ud.until_date IS NULL OR CURRENT_DATE <= ud.until_date)
                                    UNION ALL
                                    SELECT dh_1.original_user_id,
                                           ud.deputy_user_id,
                                           dh_1.depth + 1                     AS depth,
                                           dh_1.path || ud.deputy_user_id::text AS path
                                    FROM user_deputies ud
                                             JOIN deputy_hierarchy dh_1
                                                  ON ud.original_user_id::text = dh_1.deputy_user_id::text
                                    WHERE ud.from_date <= CURRENT_DATE
                                      AND (ud.until_date IS NULL OR CURRENT_DATE <= ud.until_date)
                                      AND NOT ud.deputy_user_id::text = ANY (dh_1.path))
SELECT DISTINCT deputy_user_id,
                original_user_id,
                depth
FROM deputy_hierarchy dh;

-- ===== Harmonize seeded storage labels =====

-- Rename only the unchanged local storage providers created by the seed.
-- Keep customized names, descriptions, paths and other providers intact.
UPDATE storage_providers
SET name = 'Lokale Dateien und Medien',
    description = 'Dieser Speicheranbieter ermöglicht das Speichern von Dateien und Medien auf dem lokalen Dateisystem des Servers.',
    updated = now()
WHERE storage_provider_definition_key = 'de.aivot.core.local_disk_storage'
  AND storage_provider_definition_version = 1
  AND type = 0
  AND NOT system_provider
  AND NOT test_provider
  AND configuration #>> '{root,value}' = './data/assets/'
  AND name = 'Lokale Dokumente & Medien'
  AND description = 'Dieser Speicheranbieter ermöglicht das Speichern von Dokumenten und Medien auf dem lokalen Dateisystem des Servers.';

UPDATE storage_providers
SET name = 'Lokale Vorgangsanlagen',
    description = 'Dieser Speicheranbieter ermöglicht das Speichern von Vorgangsanlagen auf dem lokalen Dateisystem des Servers.',
    updated = now()
WHERE storage_provider_definition_key = 'de.aivot.core.local_disk_storage'
  AND storage_provider_definition_version = 1
  AND type = 1
  AND NOT system_provider
  AND NOT test_provider
  AND configuration #>> '{root,value}' = './data/attachments/'
  AND name = 'Lokale Vorgangsdokumente'
  AND description = 'Dieser Speicheranbieter ermöglicht das Speichern von Vorgangsdokumenten auf dem lokalen Dateisystem des Servers.';

-- ===== Code list column mappings =====

-- Code list columns are referenced through stable keys instead of their position, so a source that reorders its
-- columns no longer changes which cells a reference reads. Value and label columns are chosen per element.
drop view if exists v_code_list_items;

alter table code_lists
    add column if not exists column_mappings jsonb default '[]'::jsonb not null,
    add column if not exists key_column_key  text;

-- Every existing column becomes a mapping whose key is the trimmed source column name. Blank names receive a
-- positional name. A key that is already used gets the suffix _2, _3, ..., like new columns during a synchronization,
-- so all keys of a list are unique.
do
$$
    declare
        code_list record;
        position  integer;
        name      text;
        key       text;
        suffix    integer;
        used_keys text[];
        mappings  jsonb;
    begin
        for code_list in select id, columns from code_lists where column_mappings = '[]'::jsonb
            loop
                used_keys := array []::text[];
                mappings := '[]'::jsonb;
                for position in 1 .. coalesce(array_length(code_list.columns, 1), 0)
                    loop
                        name := coalesce(nullif(trim(code_list.columns[position]), ''), 'COL' || (position - 1));
                        key := name;
                        suffix := 2;
                        while key = any (used_keys)
                            loop
                                key := name || '_' || suffix;
                                suffix := suffix + 1;
                            end loop;
                        used_keys := used_keys || key;
                        mappings := mappings || jsonb_build_object('key', key, 'name', name, 'index', position);
                    end loop;
                update code_lists set column_mappings = mappings where id = code_list.id;
            end loop;
    end
$$;

-- The former value column identifies the rows, e.g. when outdated rows are kept during a synchronization.
update code_lists cl
set key_column_key = (select mapping ->> 'key'
                      from jsonb_array_elements(cl.column_mappings) mapping
                      where (mapping ->> 'index')::integer = cl.value_column_index + 1)
where cl.key_column_key is null;

alter table code_lists
    drop column if exists value_column_index,
    drop column if exists label_column_index;

-- ===== Optimistic locking memberships =====

-- Optimistic locking for role changes of department and team members: the role dialogs save the role set of a
-- membership in one request, which checks and raises the lock version of the membership row.
alter table department_memberships
    add column if not exists lock_version bigint not null default 0;

alter table team_memberships
    add column if not exists lock_version bigint not null default 0;

-- The edit dialogs take the lock version from these list views, so they expose it as their last column.

create or replace view v_user_deputy_with_details(id, from_date, until_date, active, original_user_id, original_user_email, original_user_first_name, original_user_last_name, original_user_enabled, original_user_verified, original_user_deleted_in_idp, original_user_system_role_id, original_user_full_name, deputy_user_id, deputy_user_email, deputy_user_first_name, deputy_user_last_name, deputy_user_enabled, deputy_user_verified, deputy_user_deleted_in_idp, deputy_user_system_role_id, deputy_user_full_name, lock_version) as
SELECT udep.id,
       udep.from_date,
       udep.until_date,
       udep.from_date <= CURRENT_DATE AND (udep.until_date IS NULL OR CURRENT_DATE <= udep.until_date) AS active,
       ou.id                                                                                           AS original_user_id,
       ou.email                                                                                        AS original_user_email,
       ou.first_name                                                                                   AS original_user_first_name,
       ou.last_name                                                                                    AS original_user_last_name,
       ou.enabled                                                                                      AS original_user_enabled,
       ou.verified                                                                                     AS original_user_verified,
       ou.deleted_in_idp                                                                               AS original_user_deleted_in_idp,
       ou.system_role_id                                                                               AS original_user_system_role_id,
       ou.full_name                                                                                    AS original_user_full_name,
       du.id                                                                                           AS deputy_user_id,
       du.email                                                                                        AS deputy_user_email,
       du.first_name                                                                                   AS deputy_user_first_name,
       du.last_name                                                                                    AS deputy_user_last_name,
       du.enabled                                                                                      AS deputy_user_enabled,
       du.verified                                                                                     AS deputy_user_verified,
       du.deleted_in_idp                                                                               AS deputy_user_deleted_in_idp,
       du.system_role_id                                                                               AS deputy_user_system_role_id,
       du.full_name                                                                                    AS deputy_user_full_name,
       udep.lock_version
FROM user_deputies udep
         JOIN users ou ON udep.original_user_id::text = ou.id::text
         JOIN users du ON udep.deputy_user_id::text = du.id::text;

create or replace view v_department_memberships_with_details(membership_id, membership_has_deputies, membership_deputies, user_id, user_email, user_first_name, user_last_name, user_full_name, user_enabled, user_verified, user_deleted_in_idp, user_system_role_id, department_id, department_name, department_postal_address, department_imprint, department_common_privacy, department_common_accessibility, department_technical_support_email, department_technical_support_phone, department_technical_support_info, department_special_support_email, department_special_support_phone, department_special_support_info, department_theme_id, department_default_mail_signature, department_depth, department_parent_department_id, department_parent_names, department_parent_ids, domain_roles, domain_role_assignments, domain_role_permissions, lock_version) as
WITH aggregated_domain_roles AS (SELECT dra.department_membership_id,
                                        jsonb_agg(dr.*)                        AS domain_roles,
                                        jsonb_agg(dra.*)                       AS domain_role_assignments,
                                        array_unique_union_agg(dr.permissions) AS domain_role_permissions
                                 FROM domain_role_assignments dra
                                          JOIN domain_roles dr ON dr.id = dra.domain_role_id
                                 GROUP BY dra.department_membership_id),
     aggregated_deputies AS (SELECT dep_1.original_user_id,
                                    jsonb_agg(usr.*) AS deputies
                             FROM v_user_is_recursively_deputy_for dep_1
                                      JOIN users usr ON dep_1.deputy_user_id::text = usr.id::text
        GROUP BY dep_1.original_user_id)
SELECT mem.id                                                           AS membership_id,
       jsonb_array_length(COALESCE(ad.deputies, '[]'::jsonb)) > 0       AS membership_has_deputies,
       COALESCE(ad.deputies, '[]'::jsonb)                               AS membership_deputies,
       original_usr.id                                                  AS user_id,
       original_usr.email                                               AS user_email,
       original_usr.first_name                                          AS user_first_name,
       original_usr.last_name                                           AS user_last_name,
       original_usr.full_name                                           AS user_full_name,
       original_usr.enabled                                             AS user_enabled,
       original_usr.verified                                            AS user_verified,
       original_usr.deleted_in_idp                                      AS user_deleted_in_idp,
       original_usr.system_role_id                                      AS user_system_role_id,
       dep.id                                                           AS department_id,
       dep.name                                                         AS department_name,
       dep.postal_address                                               AS department_postal_address,
       dep.imprint                                                      AS department_imprint,
       dep.common_privacy                                               AS department_common_privacy,
       dep.common_accessibility                                         AS department_common_accessibility,
       dep.technical_support_email                                      AS department_technical_support_email,
       dep.technical_support_phone                                      AS department_technical_support_phone,
       dep.technical_support_info                                       AS department_technical_support_info,
       dep.special_support_email                                        AS department_special_support_email,
       dep.special_support_phone                                        AS department_special_support_phone,
       dep.special_support_info                                         AS department_special_support_info,
       dep.theme_id                                                     AS department_theme_id,
       dep.default_mail_signature                                       AS department_default_mail_signature,
       dep.depth                                                        AS department_depth,
       dep.parent_department_id                                         AS department_parent_department_id,
       dep.parent_names                                                 AS department_parent_names,
       dep.parent_ids                                                   AS department_parent_ids,
       COALESCE(adr.domain_roles, '[]'::jsonb)                          AS domain_roles,
       COALESCE(adr.domain_role_assignments, '[]'::jsonb)               AS domain_role_assignments,
       COALESCE(adr.domain_role_permissions, '{}'::character varying[]) AS domain_role_permissions,
       mem.lock_version
FROM department_memberships mem
         LEFT JOIN v_departments_shadowed dep ON dep.id = mem.department_id
         LEFT JOIN users original_usr ON original_usr.id::text = mem.user_id::text
         LEFT JOIN aggregated_deputies ad ON ad.original_user_id::text = mem.user_id::text
    LEFT JOIN aggregated_domain_roles adr ON adr.department_membership_id = mem.id;

create or replace view v_team_memberships_with_details(membership_id, membership_has_deputies, membership_deputies, user_id, user_email, user_first_name, user_last_name, user_full_name, user_enabled, user_verified, user_deleted_in_idp, user_system_role_id, team_id, team_name, domain_roles, domain_role_assignments, domain_role_permissions, lock_version) as
WITH aggregated_roles AS (SELECT dra.team_membership_id,
                                 jsonb_agg(dr.*)                        AS domain_roles,
                                 jsonb_agg(dra.*)                       AS domain_role_assignments,
                                 array_unique_union_agg(dr.permissions) AS domain_role_permissions
                          FROM domain_role_assignments dra
                                   JOIN domain_roles dr ON dr.id = dra.domain_role_id
                          GROUP BY dra.team_membership_id),
     aggregated_deputies AS (SELECT dep.original_user_id,
                                    jsonb_agg(usr.*) AS deputies
                             FROM v_user_is_recursively_deputy_for dep
                                      JOIN users usr ON dep.deputy_user_id::text = usr.id::text
        GROUP BY dep.original_user_id)
SELECT mem.id                                                          AS membership_id,
       jsonb_array_length(COALESCE(ad.deputies, '[]'::jsonb)) > 0      AS membership_has_deputies,
       COALESCE(ad.deputies, '[]'::jsonb)                              AS membership_deputies,
       original_usr.id                                                 AS user_id,
       original_usr.email                                              AS user_email,
       original_usr.first_name                                         AS user_first_name,
       original_usr.last_name                                          AS user_last_name,
       original_usr.full_name                                          AS user_full_name,
       original_usr.enabled                                            AS user_enabled,
       original_usr.verified                                           AS user_verified,
       original_usr.deleted_in_idp                                     AS user_deleted_in_idp,
       original_usr.system_role_id                                     AS user_system_role_id,
       team.id                                                         AS team_id,
       team.name                                                       AS team_name,
       COALESCE(ar.domain_roles, '[]'::jsonb)                          AS domain_roles,
       COALESCE(ar.domain_role_assignments, '[]'::jsonb)               AS domain_role_assignments,
       COALESCE(ar.domain_role_permissions, '{}'::character varying[]) AS domain_role_permissions,
       mem.lock_version
FROM team_memberships mem
         LEFT JOIN teams team ON team.id = mem.team_id
         LEFT JOIN users original_usr ON original_usr.id::text = mem.user_id::text
         LEFT JOIN aggregated_deputies ad ON ad.original_user_id::text = mem.user_id::text
    LEFT JOIN aggregated_roles ar ON ar.team_membership_id = mem.id;

-- ===== Require fit connect provider selections =====

-- Make the former runtime defaults explicit before both selections become required.
-- Preserve configured values and unrelated provider settings.
UPDATE communication_providers
SET configuration = jsonb_set(configuration, '{environment}', '{"type":"Literal","value":"TEST"}'::jsonb, true)
WHERE communication_provider_definition_key = 'de.aivot.core.fit_connect_zbp_communication_provider'
  AND communication_provider_definition_version = 1
  AND jsonb_typeof(configuration) = 'object'
  AND (
      NOT (configuration ? 'environment')
      OR configuration -> 'environment' = 'null'::jsonb
      OR (
          configuration #>> '{environment,type}' = 'Literal'
          AND configuration #>> '{environment,value}' IS NULL
      )
  );

UPDATE communication_providers
SET configuration = jsonb_set(configuration, '{senderDestinationType}', '{"type":"Literal","value":"ONLINE_SERVICE"}'::jsonb, true)
WHERE communication_provider_definition_key = 'de.aivot.core.fit_connect_zbp_communication_provider'
  AND communication_provider_definition_version = 1
  AND jsonb_typeof(configuration) = 'object'
  AND (
      NOT (configuration ? 'senderDestinationType')
      OR configuration -> 'senderDestinationType' = 'null'::jsonb
      OR (
          configuration #>> '{senderDestinationType,type}' = 'Literal'
          AND configuration #>> '{senderDestinationType,value}' IS NULL
      )
  );
