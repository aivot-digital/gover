-- Remove views depending on forms.public_title before altering the table
DROP VIEW IF EXISTS search_items;
DROP VIEW IF EXISTS submissions_with_memberships;
DROP VIEW IF EXISTS form_with_memberships;
DROP VIEW IF EXISTS form_versions_with_memberships;
DROP VIEW IF EXISTS user_form_version_permissions;
DROP VIEW IF EXISTS form_version_with_details;

-- Add a new column to form_versions with the public title
ALTER TABLE form_versions
    ADD COLUMN public_title TEXT;

-- Copy the public title from forms to form_versions
UPDATE form_versions fv
SET public_title = f.public_title
FROM forms f
WHERE fv.form_id = f.id;

-- Add a NOT NULL constraint to the new column
ALTER TABLE form_versions
    ALTER COLUMN public_title SET NOT NULL;

-- Remove the public title column from forms
ALTER TABLE forms
    DROP COLUMN public_title;

--
create view form_version_with_details as
select fms.id,
       fms.slug,
       fms.internal_title,
       fms.developing_department_id,
       fms.managing_department_id,
       fms.responsible_department_id,
       fms.published_version,
       fms.drafted_version,
       fms.version_count,
       vers.*
from form_versions vers
         inner join forms as fms on fms.id = vers.form_id;

create view user_form_version_permissions as
select fms.id                                                      as form_id,
       fms.version                                                 as form_version,
       mems.user_id                                                as user_id,
       bool_or(mems.department_id = fms.developing_department_id)  as user_is_developer,
       bool_or(mems.department_id = fms.managing_department_id)    as user_is_manager,
       bool_or(mems.department_id = fms.responsible_department_id) as user_is_responsible
from form_version_with_details as fms
         inner join department_memberships as mems
                    on fms.developing_department_id = mems.department_id
                        or fms.managing_department_id = mems.department_id
                        or fms.responsible_department_id = mems.department_id
group by fms.id, fms.version, mems.user_id;

create view form_with_memberships as
select fms.*,

       usrs.id                                                     as user_id,
       usrs.email                                                  as user_email,
       usrs.first_name                                             as user_first_name,
       usrs.last_name                                              as user_last_name,
       usrs.full_name                                              as user_full_name,
       usrs.enabled                                                as user_enabled,
       usrs.verified                                               as user_verified,
       usrs.global_admin                                           as user_global_admin,
       usrs.deleted_in_idp                                         as user_deleted_in_idp,

       bool_or(mems.department_id = fms.developing_department_id)  as user_is_developer,
       bool_or(mems.department_id = fms.managing_department_id)    as user_is_manager,
       bool_or(mems.department_id = fms.responsible_department_id) as user_is_responsible
from forms as fms
         inner join department_memberships as mems
                    on fms.developing_department_id = mems.department_id
                        or fms.managing_department_id = mems.department_id
                        or fms.responsible_department_id = mems.department_id
         inner join users as usrs
                    on usrs.id = mems.user_id
group by fms.id, usrs.id;

create view form_versions_with_memberships as
select fms.*,

       usrs.id             as user_id,
       usrs.email          as user_email,
       usrs.first_name     as user_first_name,
       usrs.last_name      as user_last_name,
       usrs.full_name      as user_full_name,
       usrs.enabled        as user_enabled,
       usrs.verified       as user_verified,
       usrs.global_admin   as user_global_admin,
       usrs.deleted_in_idp as user_deleted_in_idp,

       perm.user_is_developer,
       perm.user_is_manager,
       perm.user_is_responsible
from form_version_with_details as fms
         inner join user_form_version_permissions as perm
                    on fms.id = perm.form_id and fms.version = perm.form_version
         inner join users as usrs
                    on usrs.id = perm.user_id;

create view submissions_with_memberships as
select subs.*,

       fms.slug                            as form_slug,
       fms.internal_title                  as form_internal_title,
       fms.public_title                    as form_public_title,
       fms.developing_department_id        as form_developing_department_id,
       fms.managing_department_id          as form_managing_department_id,
       fms.responsible_department_id       as form_responsible_department_id,
       fms.created                         as form_created,
       fms.updated                         as form_updated,
       fms.published_version               as form_published_version,
       fms.drafted_version                 as form_drafted_version,
       fms.version_count                   as form_version_count,
       fms.status                          as form_status,
       fms.type                            as form_type,
       fms.legal_support_department_id     as form_legal_support_department_id,
       fms.technical_support_department_id as form_technical_support_department_id,
       fms.imprint_department_id           as form_imprint_department_id,
       fms.privacy_department_id           as form_privacy_department_id,
       fms.accessibility_department_id     as form_accessibility_department_id,
       fms.customer_access_hours           as form_customer_access_hours,
       fms.submission_retention_weeks      as form_submission_retention_weeks,
       fms.theme_id                        as form_theme_id,
       fms.pdf_template_key                as form_pdf_template_key,
       fms.payment_provider_key            as form_payment_provider_key,
       fms.payment_purpose                 as form_payment_purpose,
       fms.payment_description             as form_payment_description,
       fms.payment_products                as form_payment_products,
       fms.identity_providers              as form_identity_providers,
       fms.identity_verification_required  as form_identity_verification_required,
       fms.destination_id                  as form_destination_id,
       fms.root_element                    as form_root_element,
       fms.published                       as form_version_published,
       fms.revoked                         as form_version_revoked,

       fms.user_id,
       fms.user_email,
       fms.user_first_name,
       fms.user_last_name,
       fms.user_full_name,
       fms.user_enabled,
       fms.user_verified,
       fms.user_global_admin,
       fms.user_deleted_in_idp,
       fms.user_is_developer,
       fms.user_is_manager,
       fms.user_is_responsible
from submissions as subs
         inner join form_versions_with_memberships as fms
                    on subs.form_id = fms.id and subs.form_version = fms.version;

CREATE VIEW search_items AS
SELECT text 'assets'                   AS origin_table,
       filename                        AS label,
       key::varchar                    AS id,
       filename                        AS search_text,
       to_tsvector('german', filename) AS search_vector
FROM assets
UNION ALL
SELECT text 'data_object_items'                                 AS origin_table,
       id                                                       AS label,
       schema_key || ',' || id                                  AS id,
       concat(id::varchar, ' ', data::varchar)                  AS search_text,
       to_tsvector('german', id) || to_tsvector('german', data) AS search_vector
FROM data_object_items
UNION ALL
SELECT text 'data_object_schemas'  AS origin_table,
       name                        AS label,
       key::varchar                AS id,
       name                        AS search_text,
       to_tsvector('german', name) AS search_vector
FROM data_object_schemas
UNION ALL
SELECT text 'departments'          AS origin_table,
       name                        AS label,
       id::varchar                 AS id,
       name                        AS search_text,
       to_tsvector('german', name) AS search_vector
FROM departments
UNION ALL
SELECT text 'destinations'         AS origin_table,
       name                        AS label,
       id::varchar                 AS id,
       name                        AS search_text,
       to_tsvector('german', name) AS search_vector
FROM destinations
UNION ALL
SELECT text 'forms'                                                                 AS origin_table,
       internal_title || ' (Version ' || version || ')'                             AS label,
       concat(id::varchar, ',', version::varchar)                                   AS id,
       concat(internal_title, ' ', public_title)                                    AS search_text,
       to_tsvector('german', internal_title) || to_tsvector('german', public_title) AS search_vector
FROM form_version_with_details
UNION ALL
SELECT text 'identity_providers'   AS origin_table,
       name                        AS label,
       key::varchar                AS id,
       name                        AS search_text,
       to_tsvector('german', name) AS search_vector
FROM identity_providers
UNION ALL
SELECT text 'payment_providers'    AS origin_table,
       name                        AS label,
       key::varchar                AS id,
       name                        AS search_text,
       to_tsvector('german', name) AS search_vector
FROM payment_providers
UNION ALL
SELECT text 'presets'                            AS origin_table,
       concat(title, ' (Version ', version, ')') AS label,
       concat(preset_key, ',', version::varchar) AS id,
       title                                     AS search_text,
       to_tsvector('german', title)              AS search_vector
FROM preset_version_with_details
UNION ALL
SELECT text 'provider_links'       AS origin_table,
       text                        AS label,
       id::varchar                 AS id,
       text                        AS search_text,
       to_tsvector('german', text) AS search_vector
FROM provider_links
UNION ALL
SELECT text 'secrets'              AS origin_table,
       name                        AS label,
       key::varchar                AS id,
       name                        AS search_text,
       to_tsvector('german', name) AS search_vector
FROM secrets
UNION ALL
SELECT text 'submissions'                                                            AS origin_table,
       file_number                                                                   AS label,
       id::varchar                                                                   AS id,
       concat(id::varchar, ' ', file_number)                                         AS search_text,
       to_tsvector('german', id) || to_tsvector('german', COALESCE(file_number, '')) AS search_vector
FROM submissions
UNION ALL
SELECT text 'themes'               AS origin_table,
       name                        AS label,
       id::varchar                 AS id,
       name                        AS search_text,
       to_tsvector('german', name) AS search_vector
FROM themes;
