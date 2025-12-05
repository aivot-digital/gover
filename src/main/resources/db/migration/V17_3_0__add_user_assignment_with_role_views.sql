-- create view for organization unit memberships with details
create view v_department_user_role_assignments_with_details as
select mems.*,

       ura.id    as user_role_assignment_id,

       urls.id   as user_role_id,
       urls.name as user_role_name,
       urls.description,
       urls.department_permission_edit,
       urls.team_permission_edit,
       urls.form_permission_create,
       urls.form_permission_read,
       urls.form_permission_edit,
       urls.form_permission_delete,
       urls.form_permission_annotate,
       urls.form_permission_publish,
       urls.process_permission_create,
       urls.process_permission_read,
       urls.process_permission_edit,
       urls.process_permission_delete,
       urls.process_permission_annotate,
       urls.process_permission_publish,
       urls.process_instance_permission_create,
       urls.process_instance_permission_read,
       urls.process_instance_permission_edit,
       urls.process_instance_permission_delete,
       urls.process_instance_permission_annotate
from user_role_assignments ura
         join user_roles urls on ura.user_role_id = urls.id
         right join v_department_memberships_with_details mems on ura.department_membership_id = mems.id
where ura.department_membership_id is not null;

-- create view for teams memberships with details
create view v_team_user_role_assignments_with_details as
select mems.*,

       ura.id    as user_role_assignment_id,

       urls.id   as user_role_id,
       urls.name as user_role_name,
       urls.description,
       urls.department_permission_edit,
       urls.team_permission_edit,
       urls.form_permission_create,
       urls.form_permission_read,
       urls.form_permission_edit,
       urls.form_permission_delete,
       urls.form_permission_annotate,
       urls.form_permission_publish,
       urls.process_permission_create,
       urls.process_permission_read,
       urls.process_permission_edit,
       urls.process_permission_delete,
       urls.process_permission_annotate,
       urls.process_permission_publish,
       urls.process_instance_permission_create,
       urls.process_instance_permission_read,
       urls.process_instance_permission_edit,
       urls.process_instance_permission_delete,
       urls.process_instance_permission_annotate
from user_role_assignments ura
         join user_roles urls on ura.user_role_id = urls.id
         right join v_team_memberships_with_details mems on ura.team_membership_id = mems.id
where ura.team_membership_id is not null;
