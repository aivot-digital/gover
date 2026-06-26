-- Create a view to get form access of each user
CREATE VIEW user_form_access AS
SELECT fms.id                                                       AS form_id,
       mems.user_id                                                 AS user_id,
       bool_or(mems.department_id = fms.developing_department_id)   AS is_developer,
       bool_or(mems.department_id = vers.managing_department_id)    AS is_manager,
       bool_or(mems.department_id = vers.responsible_department_id) AS is_responsible
FROM forms AS fms
         INNER JOIN form_versions AS vers
                    ON fms.id = vers.form_id
         INNER JOIN department_memberships AS mems
                    ON fms.developing_department_id = mems.department_id
                        OR vers.managing_department_id = mems.department_id
                        OR vers.responsible_department_id = mems.department_id
GROUP BY fms.id, mems.user_id;

-- Create a view to determine access of each user to the forms
CREATE VIEW user_form_version_access AS
SELECT fms.id                                                       AS form_id,
       vers.version                                                 AS form_version,
       mems.user_id                                                 AS user_id,
       bool_or(mems.department_id = fms.developing_department_id)   AS is_developer,
       bool_or(mems.department_id = vers.managing_department_id)    AS is_manager,
       bool_or(mems.department_id = vers.responsible_department_id) AS is_responsible
FROM forms AS fms
         INNER JOIN form_versions as vers
                    ON fms.id = vers.form_id
         INNER JOIN department_memberships as mems
                    ON fms.developing_department_id = mems.department_id
                        OR vers.managing_department_id = mems.department_id
                        OR vers.responsible_department_id = mems.department_id
GROUP BY fms.id, vers.version, mems.user_id;

-- Recreate the view of the form versions with the details from the parent form
CREATE VIEW form_version_with_details AS
SELECT fms.id,
       fms.slug,
       fms.internal_title,
       fms.developing_department_id,
       fms.published_version,
       fms.drafted_version,
       fms.version_count,

       vers.*
FROM forms fms
         INNER JOIN form_versions AS vers ON fms.id = vers.form_id;

-- Create a view to get forms with user memberships
CREATE VIEW form_with_memberships AS
SELECT fms.*,

       usrs.id                                                      AS user_id,
       usrs.email                                                   AS user_email,
       usrs.first_name                                              AS user_first_name,
       usrs.last_name                                               AS user_last_name,
       usrs.full_name                                               AS user_full_name,
       usrs.enabled                                                 AS user_enabled,
       usrs.verified                                                AS user_verified,
       usrs.global_admin                                            AS user_global_admin,
       usrs.deleted_in_idp                                          AS user_deleted_in_idp,

       bool_or(mems.department_id = fms.developing_department_id)   AS user_is_developer,
       bool_or(mems.department_id = vers.managing_department_id)    AS user_is_manager,
       bool_or(mems.department_id = vers.responsible_department_id) AS user_is_responsible
FROM forms AS fms
         INNER JOIN form_versions as vers
                    ON fms.id = vers.form_id
         INNER JOIN department_memberships AS mems
                    ON fms.developing_department_id = mems.department_id
                        OR vers.managing_department_id = mems.department_id
                        OR vers.responsible_department_id = mems.department_id
         INNER JOIN users AS usrs
                    ON usrs.id = mems.user_id
GROUP BY fms.id, vers.version, usrs.id;

-- Create a view to get form versions with user memberships
CREATE VIEW form_versions_with_memberships AS
SELECT fms.id,
       fms.slug,
       fms.internal_title,
       fms.developing_department_id,
       fms.published_version,
       fms.drafted_version,
       fms.version_count,

       vers.*,

       usrs.id                                                      as user_id,
       usrs.email                                                   as user_email,
       usrs.first_name                                              as user_first_name,
       usrs.last_name                                               as user_last_name,
       usrs.full_name                                               as user_full_name,
       usrs.enabled                                                 as user_enabled,
       usrs.verified                                                as user_verified,
       usrs.global_admin                                            as user_global_admin,
       usrs.deleted_in_idp                                          as user_deleted_in_idp,

       bool_or(mems.department_id = fms.developing_department_id)   as user_is_developer,
       bool_or(mems.department_id = vers.managing_department_id)    as user_is_manager,
       bool_or(mems.department_id = vers.responsible_department_id) as user_is_responsible
from forms as fms
         inner join form_versions as vers
                    on fms.id = vers.form_id
         inner join department_memberships as mems
                    on fms.developing_department_id = mems.department_id
                        or vers.managing_department_id = mems.department_id
                        or vers.responsible_department_id = mems.department_id
         inner join users as usrs
                    on usrs.id = mems.user_id
group by fms.id, vers.version, vers.form_id, usrs.id;

-- Create a view to get submissions with form version memberships
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
