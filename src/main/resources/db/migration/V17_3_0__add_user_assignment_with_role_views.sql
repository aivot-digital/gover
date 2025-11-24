-- create view for organization unit memberships with details
create view v_org_user_role_assignments_with_details as
select ura.id                                    as user_role_assignment_id,

       ur.id                                     as user_role_id,
       ur.name                                   as user_role_name,
       ur.description                            as user_role_description,
       ur.org_unit_member_permission_edit        as user_role_org_unit_member_permission_edit,
       ur.team_member_permission_edit            as user_role_team_member_permission_edit,
       ur.form_permission_create                 as user_role_form_permission_create,
       ur.form_permission_read                   as user_role_form_permission_read,
       ur.form_permission_edit                   as user_role_form_permission_edit,
       ur.form_permission_delete                 as user_role_form_permission_delete,
       ur.form_permission_annotate               as user_role_form_permission_annotate,
       ur.form_permission_publish                as user_role_form_permission_publish,
       ur.process_permission_create              as user_role_process_permission_create,
       ur.process_permission_read                as user_role_process_permission_read,
       ur.process_permission_edit                as user_role_process_permission_edit,
       ur.process_permission_delete              as user_role_process_permission_delete,
       ur.process_permission_annotate            as user_role_process_permission_annotate,
       ur.process_permission_publish             as user_role_process_permission_publish,
       ur.process_instance_permission_create     as user_role_process_instance_permission_create,
       ur.process_instance_permission_read       as user_role_process_instance_permission_read,
       ur.process_instance_permission_edit       as user_role_process_instance_permission_edit,
       ur.process_instance_permission_delete     as user_role_process_instance_permission_delete,
       ur.process_instance_permission_annotate   as user_role_process_instance_permission_annotate,

       om.membership_id                          as org_unit_membership_id,
       om.organizational_unit_id                 as org_unit_membership_organizational_unit_id,
       om.organizational_unit_name               as org_unit_membership_organizational_unit_name,
       om.organizational_unit_parent_org_unit_id as org_unit_membership_organizational_unit_parent_org_unit_id,
       om.organizational_unit_depth              as org_unit_membership_organizational_unit_depth,
       om.user_id                                as org_unit_membership_user_id,
       om.user_first_name                        as org_unit_membership_user_first_name,
       om.user_last_name                         as org_unit_membership_user_last_name,
       om.user_full_name                         as org_unit_membership_user_full_name,
       om.user_email                             as org_unit_membership_user_email,
       om.user_enabled                           as org_unit_membership_user_enabled,
       om.user_verified                          as org_unit_membership_user_verified,
       om.user_global_admin                      as org_unit_membership_user_global_admin,
       om.user_deleted_in_idp                    as org_unit_membership_user_deleted_in_idp
from user_role_assignments ura
         join user_roles ur on ura.user_role_id = ur.id
         right join v_organizational_unit_memberships_with_details om on ura.organizational_unit_membership_id = om.membership_id
where ura.organizational_unit_membership_id is not null;

-- create view for teams memberships with details
create view v_team_user_role_assignments_with_details as
select ura.id                                  as user_role_assignment_id,

       ur.id                                   as user_role_id,
       ur.name                                 as user_role_name,
       ur.description                          as user_role_description,
       ur.org_unit_member_permission_edit      as user_role_org_unit_member_permission_edit,
       ur.team_member_permission_edit          as user_role_team_member_permission_edit,
       ur.form_permission_create               as user_role_form_permission_create,
       ur.form_permission_read                 as user_role_form_permission_read,
       ur.form_permission_edit                 as user_role_form_permission_edit,
       ur.form_permission_delete               as user_role_form_permission_delete,
       ur.form_permission_annotate             as user_role_form_permission_annotate,
       ur.form_permission_publish              as user_role_form_permission_publish,
       ur.process_permission_create            as user_role_process_permission_create,
       ur.process_permission_read              as user_role_process_permission_read,
       ur.process_permission_edit              as user_role_process_permission_edit,
       ur.process_permission_delete            as user_role_process_permission_delete,
       ur.process_permission_annotate          as user_role_process_permission_annotate,
       ur.process_permission_publish           as user_role_process_permission_publish,
       ur.process_instance_permission_create   as user_role_process_instance_permission_create,
       ur.process_instance_permission_read     as user_role_process_instance_permission_read,
       ur.process_instance_permission_edit     as user_role_process_instance_permission_edit,
       ur.process_instance_permission_delete   as user_role_process_instance_permission_delete,
       ur.process_instance_permission_annotate as user_role_process_instance_permission_annotate,

       om.membership_id                        as team_membership_id,
       om.team_id                              as team_membership_team_id,
       om.team_name                            as team_membership_team_name,
       om.user_id                              as team_membership_user_id,
       om.user_first_name                      as team_membership_user_first_name,
       om.user_last_name                       as team_membership_user_last_name,
       om.user_full_name                       as team_membership_user_full_name,
       om.user_email                           as team_membership_user_email,
       om.user_enabled                         as team_membership_user_enabled,
       om.user_verified                        as team_membership_user_verified,
       om.user_global_admin                    as team_membership_user_global_admin,
       om.user_deleted_in_idp                  as team_membership_user_deleted_in_idp
from user_role_assignments ura
         join user_roles ur on ura.user_role_id = ur.id
         right join v_team_memberships_with_details om on ura.team_membership_id = om.membership_id
where ura.team_membership_id is not null;
