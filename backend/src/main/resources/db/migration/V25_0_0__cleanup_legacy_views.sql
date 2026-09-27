-- TODO: drop view search_items;
-- TODO: drop view form_version_with_details;
-- TODO: drop view user_form_access;
-- TODO: drop view user_form_version_access;
create view v_department_memberships_with_permissions_leg as
select dms.*,
       false as department_permission_edit,
       false as form_permission_create,
       false as form_permission_read,
       false as form_permission_edit,
       false as form_permission_delete,
       false as form_permission_annotate,
       false as form_permission_publish,
       false as process_permission_create,
       false as process_permission_read,
       false as process_permission_edit,
       false as process_permission_delete,
       false as process_permission_annotate,
       false as process_permission_publish,
       false as process_instance_permission_create,
       false as process_instance_permission_read,
       false as process_instance_permission_edit,
       false as process_instance_permission_delete,
       false as process_instance_permission_annotate
from department_memberships dms;

create view v_department_user_role_assignments_with_details as
select *
from domain_role_assignments;

-- create a view to determine user access to org units
create view v_user_form_permissions as
select accesses.form_id                            as form_id,
       accesses.user_id                            as user_id,

       bool_and(accesses.form_permission_create)   as form_permission_create,
       bool_and(accesses.form_permission_read)     as form_permission_read,
       bool_and(accesses.form_permission_edit)     as form_permission_edit,
       bool_and(accesses.form_permission_delete)   as form_permission_delete,
       bool_and(accesses.form_permission_annotate) as form_permission_annotate,
       bool_and(accesses.form_permission_publish)  as form_permission_publish
from (select f.id                         as form_id,
             mem.user_id                  as user_id,

             mem.form_permission_create   as form_permission_create,
             mem.form_permission_read     as form_permission_read,
             mem.form_permission_edit     as form_permission_edit,
             mem.form_permission_delete   as form_permission_delete,
             mem.form_permission_annotate as form_permission_annotate,
             mem.form_permission_publish  as form_permission_publish
      from forms as f
               join v_department_memberships_with_permissions_leg mem on mem.department_id = f.developing_department_id
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
               join department_memberships mem on
          mem.department_id = f.developing_department_id) as accesses
group by form_id, user_id;

-- create view for accessible forms
create view v_forms_with_permissions as
select fms.*,

       perms.user_id                  as user_id,

       perms.form_permission_create   as form_permission_create,
       perms.form_permission_read     as form_permission_read,
       perms.form_permission_edit     as form_permission_edit,
       perms.form_permission_delete   as form_permission_delete,
       perms.form_permission_annotate as form_permission_annotate,
       perms.form_permission_publish  as form_permission_publish
from forms fms
         join v_user_form_permissions perms on fms.id = perms.form_id;

-- create a view for form versions with the form details
create view v_form_versions_with_details as
select fms.id,
       fms.slug,
       fms.internal_title,
       fms.developing_department_id,
       fms.published_version,
       fms.drafted_version,
       fms.version_count,

       vers.*
from forms fms
         join form_versions vers on fms.id = vers.form_id;

-- create a view for accessible forms with details
create view v_form_versions_with_details_and_permissions as
select fms.*,

       perms.user_id                  as user_id,

       perms.form_permission_create   as form_permission_create,
       perms.form_permission_read     as form_permission_read,
       perms.form_permission_edit     as form_permission_edit,
       perms.form_permission_delete   as form_permission_delete,
       perms.form_permission_annotate as form_permission_annotate,
       perms.form_permission_publish  as form_permission_publish
from v_form_versions_with_details fms
         join v_user_form_permissions perms on fms.form_id = perms.form_id;