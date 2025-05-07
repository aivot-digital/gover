-- create a view of all accessible forms for a user
drop view forms_with_memberships;
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

-- create a view of all accessible submissions for a user
drop view submissions_with_memberships;
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