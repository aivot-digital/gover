-- create view with permissions for organizational unit memberships
create view v_department_memberships_with_permissions as
select oum.*,

       bool_or(ur.department_permission_edit)           as department_permission_edit,

       bool_or(ur.form_permission_create)               as form_permission_create,
       bool_or(ur.form_permission_read)                 as form_permission_read,
       bool_or(ur.form_permission_edit)                 as form_permission_edit,
       bool_or(ur.form_permission_delete)               as form_permission_delete,
       bool_or(ur.form_permission_annotate)             as form_permission_annotate,
       bool_or(ur.form_permission_publish)              as form_permission_publish,

       bool_or(ur.process_permission_create)            as process_permission_create,
       bool_or(ur.process_permission_read)              as process_permission_read,
       bool_or(ur.process_permission_edit)              as process_permission_edit,
       bool_or(ur.process_permission_delete)            as process_permission_delete,
       bool_or(ur.process_permission_annotate)          as process_permission_annotate,
       bool_or(ur.process_permission_publish)           as process_permission_publish,

       bool_or(ur.process_instance_permission_create)   as process_instance_permission_create,
       bool_or(ur.process_instance_permission_read)     as process_instance_permission_read,
       bool_or(ur.process_instance_permission_edit)     as process_instance_permission_edit,
       bool_or(ur.process_instance_permission_delete)   as process_instance_permission_delete,
       bool_or(ur.process_instance_permission_annotate) as process_instance_permission_annotate
from department_memberships oum
         join user_role_assignments ura
              on oum.id = ura.department_membership_id
         join user_roles ur on ura.user_role_id = ur.id
group by oum.id, oum.department_id, oum.user_id;

-- create view with permissions for team memberships
create view v_team_memberships_with_permissions as
select tm.*,

       bool_or(ur.team_permission_edit)                 as team_permission_edit,

       bool_or(ur.form_permission_create)               as form_permission_create,
       bool_or(ur.form_permission_read)                 as form_permission_read,
       bool_or(ur.form_permission_edit)                 as form_permission_edit,
       bool_or(ur.form_permission_delete)               as form_permission_delete,
       bool_or(ur.form_permission_annotate)             as form_permission_annotate,
       bool_or(ur.form_permission_publish)              as form_permission_publish,

       bool_or(ur.process_permission_create)            as process_permission_create,
       bool_or(ur.process_permission_read)              as process_permission_read,
       bool_or(ur.process_permission_edit)              as process_permission_edit,
       bool_or(ur.process_permission_delete)            as process_permission_delete,
       bool_or(ur.process_permission_annotate)          as process_permission_annotate,
       bool_or(ur.process_permission_publish)           as process_permission_publish,

       bool_or(ur.process_instance_permission_create)   as process_instance_permission_create,
       bool_or(ur.process_instance_permission_read)     as process_instance_permission_read,
       bool_or(ur.process_instance_permission_edit)     as process_instance_permission_edit,
       bool_or(ur.process_instance_permission_delete)   as process_instance_permission_delete,
       bool_or(ur.process_instance_permission_annotate) as process_instance_permission_annotate
from team_memberships tm
         join user_role_assignments ura
              on tm.id = ura.team_membership_id
         join user_roles ur on ura.user_role_id = ur.id
group by tm.id, tm.team_id, tm.user_id;

-- create combined view with permissions for all memberships
create view v_memberships_with_permissions as
select memberships.department_id                                 as department_id,
       memberships.team_id                                       as team_id,
       memberships.user_id                                       as user_id,

       bool_or(memberships.department_permission_edit)           as department_permission_edit,
       bool_or(memberships.team_member_permission_edit)          as team_member_permission_edit,

       bool_or(memberships.form_permission_create)               as form_permission_create,
       bool_or(memberships.form_permission_read)                 as form_permission_read,
       bool_or(memberships.form_permission_edit)                 as form_permission_edit,
       bool_or(memberships.form_permission_delete)               as form_permission_delete,
       bool_or(memberships.form_permission_annotate)             as form_permission_annotate,
       bool_or(memberships.form_permission_publish)              as form_permission_publish,

       bool_or(memberships.process_permission_create)            as process_permission_create,
       bool_or(memberships.process_permission_read)              as process_permission_read,
       bool_or(memberships.process_permission_edit)              as process_permission_edit,
       bool_or(memberships.process_permission_delete)            as process_permission_delete,
       bool_or(memberships.process_permission_annotate)          as process_permission_annotate,
       bool_or(memberships.process_permission_publish)           as process_permission_publish,

       bool_or(memberships.process_instance_permission_create)   as process_instance_permission_create,
       bool_or(memberships.process_instance_permission_read)     as process_instance_permission_read,
       bool_or(memberships.process_instance_permission_edit)     as process_instance_permission_edit,
       bool_or(memberships.process_instance_permission_delete)   as process_instance_permission_delete,
       bool_or(memberships.process_instance_permission_annotate) as process_instance_permission_annotate
from (select oum.department_id                        as department_id,
             null                                     as team_id,
             oum.user_id                              as user_id,

             oum.department_permission_edit           as department_permission_edit,
             false                                    as team_member_permission_edit,

             oum.form_permission_create               as form_permission_create,
             oum.form_permission_read                 as form_permission_read,
             oum.form_permission_edit                 as form_permission_edit,
             oum.form_permission_delete               as form_permission_delete,
             oum.form_permission_annotate             as form_permission_annotate,
             oum.form_permission_publish              as form_permission_publish,

             oum.process_permission_create            as process_permission_create,
             oum.process_permission_read              as process_permission_read,
             oum.process_permission_edit              as process_permission_edit,
             oum.process_permission_delete            as process_permission_delete,
             oum.process_permission_annotate          as process_permission_annotate,
             oum.process_permission_publish           as process_permission_publish,

             oum.process_instance_permission_create   as process_instance_permission_create,
             oum.process_instance_permission_read     as process_instance_permission_read,
             oum.process_instance_permission_edit     as process_instance_permission_edit,
             oum.process_instance_permission_delete   as process_instance_permission_delete,
             oum.process_instance_permission_annotate as process_instance_permission_annotate
      from v_department_memberships_with_permissions oum
      union
      select null                                    as department_id,
             tm.team_id                              as team_id,
             tm.user_id                              as user_id,

             false                                   as department_permission_edit,
             tm.team_permission_edit                 as team_permission_edit,

             tm.form_permission_create               as form_permission_create,
             tm.form_permission_read                 as form_permission_read,
             tm.form_permission_edit                 as form_permission_edit,
             tm.form_permission_delete               as form_permission_delete,
             tm.form_permission_annotate             as form_permission_annotate,
             tm.form_permission_publish              as form_permission_publish,

             tm.process_permission_create            as process_permission_create,
             tm.process_permission_read              as process_permission_read,
             tm.process_permission_edit              as process_permission_edit,
             tm.process_permission_delete            as process_permission_delete,
             tm.process_permission_annotate          as process_permission_annotate,
             tm.process_permission_publish           as process_permission_publish,

             tm.process_instance_permission_create   as process_instance_permission_create,
             tm.process_instance_permission_read     as process_instance_permission_read,
             tm.process_instance_permission_edit     as process_instance_permission_edit,
             tm.process_instance_permission_delete   as process_instance_permission_delete,
             tm.process_instance_permission_annotate as process_instance_permission_annotate
      from v_team_memberships_with_permissions tm) as memberships
group by memberships.team_id, memberships.department_id, memberships.user_id;
