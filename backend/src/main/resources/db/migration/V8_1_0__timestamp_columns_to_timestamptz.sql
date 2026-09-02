-- Convert legacy timestamp columns introduced before V9 after the V8 migration boundary.
-- Existing timestamp values are interpreted in the current database session timezone.

drop view submissions_with_memberships;
drop view forms_with_memberships;
drop view departments_with_memberships;

alter table departments
    alter column created type timestamptz using created at time zone current_setting('TimeZone'),
    alter column updated type timestamptz using updated at time zone current_setting('TimeZone');

alter table provider_links
    alter column created type timestamptz using created at time zone current_setting('TimeZone'),
    alter column updated type timestamptz using updated at time zone current_setting('TimeZone');

alter table destinations
    alter column created type timestamptz using created at time zone current_setting('TimeZone'),
    alter column updated type timestamptz using updated at time zone current_setting('TimeZone');

alter table presets
    alter column created type timestamptz using created at time zone current_setting('TimeZone'),
    alter column updated type timestamptz using updated at time zone current_setting('TimeZone');

alter table forms
    alter column created type timestamptz using created at time zone current_setting('TimeZone'),
    alter column updated type timestamptz using updated at time zone current_setting('TimeZone');

alter table submissions
    alter column updated drop default;

alter table submissions
    alter column created type timestamptz using created at time zone current_setting('TimeZone'),
    alter column archived type timestamptz using archived at time zone current_setting('TimeZone'),
    alter column destination_timestamp type timestamptz using destination_timestamp at time zone current_setting('TimeZone'),
    alter column updated type timestamptz using updated at time zone current_setting('TimeZone');

alter table submissions
    alter column updated set default current_timestamp;

alter table preset_versions
    alter column published_at type timestamptz using published_at at time zone current_setting('TimeZone'),
    alter column published_store_at type timestamptz using published_store_at at time zone current_setting('TimeZone'),
    alter column created type timestamptz using created at time zone current_setting('TimeZone'),
    alter column updated type timestamptz using updated at time zone current_setting('TimeZone');

alter table assets
    alter column created type timestamptz using created at time zone current_setting('TimeZone');

alter table form_revisions
    alter column timestamp type timestamptz using timestamp at time zone current_setting('TimeZone');

alter table payment_transactions
    alter column created type timestamptz using created at time zone current_setting('TimeZone'),
    alter column updated type timestamptz using updated at time zone current_setting('TimeZone');

create view departments_with_memberships as
select deps.id,
       deps.name,
       deps.address,
       deps.imprint,
       deps.privacy,
       deps.accessibility,
       deps.technical_support_address,
       deps.special_support_address,
       deps.created,
       deps.updated,
       deps.department_mail,
       mems.id             as membership_id,
       mems.role           as membership_role,
       usrs.id             as user_id,
       usrs.email          as user_email,
       usrs.first_name     as user_first_name,
       usrs.last_name      as user_last_name,
       usrs.full_name      as user_full_name,
       usrs.enabled        as user_enabled,
       usrs.verified       as user_verified,
       usrs.global_admin   as user_global_admin,
       usrs.deleted_in_idp as user_deleted_in_idp
from departments as deps
         join department_memberships as mems
              on deps.id = mems.department_id
         join users as usrs
              on usrs.id = mems.user_id;

create view forms_with_memberships as
select fms.id,
       fms.slug,
       fms.version,
       fms.title,
       fms.status,
       fms.root,
       fms.destination_id,
       fms.legal_support_department_id,
       fms.technical_support_department_id,
       fms.imprint_department_id,
       fms.privacy_department_id,
       fms.accessibility_department_id,
       fms.developing_department_id,
       fms.managing_department_id,
       fms.responsible_department_id,
       fms.customer_access_hours,
       fms.submission_deletion_weeks,
       fms.created,
       fms.updated,
       fms.theme_id,
       fms.identity_required,
       fms.identity_providers,
       fms.pdf_body_template_key,
       fms.products,
       fms.payment_purpose,
       fms.payment_provider,
       fms.payment_description,
       fms.type,
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
         join department_memberships as mems
              on fms.developing_department_id = mems.department_id
                  or fms.managing_department_id = mems.department_id
                  or fms.responsible_department_id = mems.department_id
         join users as usrs
              on usrs.id = mems.user_id
group by fms.id, usrs.id;

create view submissions_with_memberships as
select subs.id,
       subs.created,
       subs.assignee_id,
       subs.archived,
       subs.file_number,
       subs.destination_id,
       subs.customer_input,
       subs.destination_success,
       subs.is_test_submission,
       subs.copy_sent,
       subs.copy_tries,
       subs.review_score,
       subs.destination_result,
       subs.destination_timestamp,
       subs.status,
       subs.updated,
       subs.payment_transaction_key,
       fms.id                                                      as form_id,
       fms.slug                                                    as form_slug,
       fms.version                                                 as form_version,
       fms.title                                                   as form_title,
       fms.status                                                  as form_status,
       fms.root                                                    as form_root,
       fms.destination_id                                          as form_destination_id,
       fms.legal_support_department_id                             as form_legal_support_department_id,
       fms.technical_support_department_id                         as form_technical_support_department_id,
       fms.imprint_department_id                                   as form_imprint_department_id,
       fms.privacy_department_id                                   as form_privacy_department_id,
       fms.accessibility_department_id                             as form_accessibility_department_id,
       fms.developing_department_id                                as form_developing_department_id,
       fms.managing_department_id                                  as form_managing_department_id,
       fms.responsible_department_id                               as form_responsible_department_id,
       fms.customer_access_hours                                   as form_customer_access_hours,
       fms.submission_deletion_weeks                               as form_submission_deletion_weeks,
       fms.created                                                 as form_created,
       fms.updated                                                 as form_updated,
       fms.theme_id                                                as form_theme_id,
       fms.identity_required                                       as form_identity_required,
       fms.identity_providers                                      as form_identity_providers,
       fms.pdf_body_template_key                                   as form_pdf_body_template_key,
       fms.products                                                as form_products,
       fms.payment_purpose                                         as form_payment_purpose,
       fms.payment_provider                                        as form_payment_provider,
       fms.payment_description                                     as form_payment_description,
       fms.type                                                    as form_type,
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
from submissions as subs
         join forms as fms
              on subs.form_id = fms.id
         join department_memberships as mems
              on (fms.developing_department_id = mems.department_id and subs.is_test_submission = true)
                  or fms.managing_department_id = mems.department_id
                  or fms.responsible_department_id = mems.department_id
         join users as usrs
              on usrs.id = mems.user_id
group by subs.id, fms.id, usrs.id;
