-- update forms with memberships view to include the type
create or replace view forms_with_memberships as
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
       fms.bund_id_enabled,
       fms.bund_id_level,
       fms.bayern_id_enabled,
       fms.bayern_id_level,
       fms.muk_enabled,
       fms.sh_id_enabled,
       fms.sh_id_level,
       fms.muk_level,
       fms.pdf_body_template_key,
       fms.products,
       fms.payment_purpose,
       fms.payment_provider,
       fms.payment_description,
       mems.user_id                                                as user_id,
       bool_or(mems.department_id = fms.developing_department_id)  as is_developer,
       bool_or(mems.department_id = fms.managing_department_id)    as is_manager,
       bool_or(mems.department_id = fms.responsible_department_id) as is_responsible,
       fms.type
from forms as fms
         join department_memberships as mems
              on fms.developing_department_id = mems.department_id
                  or fms.managing_department_id = mems.department_id
                  or fms.responsible_department_id = mems.department_id
group by fms.id, mems.user_id;
