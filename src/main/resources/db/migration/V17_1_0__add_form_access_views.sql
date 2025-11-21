-- create a view to determine user access to org units
create view v_form_permissions as
select accesses.form_id                            as form_id,
       accesses.user_id                            as user_id,

       bool_and(accesses.form_permission_create)   as form_permission_create,
       bool_and(accesses.form_permission_read)     as form_permission_read,
       bool_and(accesses.form_permission_edit)     as form_permission_edit,
       bool_and(accesses.form_permission_delete)   as form_permission_delete,
       bool_and(accesses.form_permission_annotate) as form_permission_annotate,
       bool_and(accesses.form_permission_publish)  as form_permission_publish
from (select f.id                                                         as form_id,
             mem.user_id                                                  as user_id,

             mem.form_permission_create or rac.form_permission_create     as form_permission_create,
             mem.form_permission_read or rac.form_permission_read         as form_permission_read,
             mem.form_permission_edit or rac.form_permission_edit         as form_permission_edit,
             mem.form_permission_delete or rac.form_permission_delete     as form_permission_delete,
             mem.form_permission_annotate or rac.form_permission_annotate as form_permission_annotate,
             mem.form_permission_publish or rac.form_permission_publish   as form_permission_publish
      from forms as f
               join resource_access_controls rac on rac.target_form_id = f.id
               join v_memberships_with_permissions mem on mem.ou_id = rac.source_org_unit_id or
                                                          mem.team_id = rac.source_team_id

      union
      select f.id        as form_id,
             mem.user_id as user_id,

             true        as form_permission_create,
             true        as form_permission_read,
             true        as form_permission_edit,
             true        as form_permission_delete,
             true        as form_permission_annotate,
             true        as form_permission_publish
      from forms as f
               join v_organizational_unit_memberships_with_permissions mem on
          mem.organizational_unit_id = f.developing_organizational_unit_id) as accesses
group by form_id, user_id;

-- create view for accessible forms
create view v_forms_with_permissions as
select fms.id                                as form_id,
       fms.slug                              as form_slug,
       fms.internal_title                    as form_internal_title,
       fms.developing_organizational_unit_id as form_developing_organizational_unit_id,
       fms.created                           as form_created,
       fms.updated                           as form_updated,
       fms.published_version                 as form_published_version,
       fms.drafted_version                   as form_drafted_version,
       fms.version_count                     as form_version_count,

       vfa.user_id                           as user_id,

       vfa.form_permission_create            as form_permission_create,
       vfa.form_permission_read              as form_permission_read,
       vfa.form_permission_edit              as form_permission_edit,
       vfa.form_permission_delete            as form_permission_delete,
       vfa.form_permission_annotate          as form_permission_annotate,
       vfa.form_permission_publish           as form_permission_publish
from forms fms
         join v_form_permissions vfa on fms.id = vfa.form_id;

-- create a view for accessible forms with details
create view v_form_versions_with_details_and_permissions as
select fms.*,

       fv.version                                  as form_version_version,
       fv.status                                   as form_version_status,
       fv.type                                     as form_version_type,
       fv.legal_support_organizational_unit_id     as form_version_legal_support_organizational_unit_id,
       fv.technical_support_organizational_unit_id as form_version_technical_support_organizational_unit_id,
       fv.imprint_organizational_unit_id           as form_version_imprint_organizational_unit_id,
       fv.privacy_organizational_unit_id           as form_version_privacy_organizational_unit_id,
       fv.accessibility_organizational_unit_id     as form_version_accessibility_organizational_unit_id,
       fv.destination_id                           as form_version_destination_id,
       fv.theme_id                                 as form_version_theme_id,
       fv.pdf_template_key                         as form_version_pdf_template_key,
       fv.payment_provider_key                     as form_version_payment_provider_key,
       fv.payment_purpose                          as form_version_payment_purpose,
       fv.payment_description                      as form_version_payment_description,
       fv.payment_products                         as form_version_payment_products,
       fv.identity_providers                       as form_version_identity_providers,
       fv.identity_verification_required           as form_version_identity_verification_required,
       fv.customer_access_hours                    as form_version_customer_access_hours,
       fv.submission_retention_weeks               as form_version_submission_retention_weeks,
       fv.root_element                             as form_version_root_element,
       fv.created                                  as form_version_created,
       fv.updated                                  as form_version_updated,
       fv.published                                as form_version_published,
       fv.revoked                                  as form_version_revoked,
       fv.public_title                             as form_version_public_title,
       fv.managing_organizational_unit_id          as form_version_managing_organizational_unit_id,
       fv.responsible_organizational_unit_id       as form_version_responsible_organizational_unit_id
from v_forms_with_permissions fms
         join form_versions fv on fms.form_id = fv.form_id
