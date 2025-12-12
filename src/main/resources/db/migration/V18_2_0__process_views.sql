-- create a view to determine user access to process definitions
create view v_user_process_definition_permissions as
select accesses.process_definition_id                 as process_definition_id,
       accesses.user_id                               as user_id,

       bool_and(accesses.process_permission_create)   as process_permission_create,
       bool_and(accesses.process_permission_read)     as process_permission_read,
       bool_and(accesses.process_permission_edit)     as process_permission_edit,
       bool_and(accesses.process_permission_delete)   as process_permission_delete,
       bool_and(accesses.process_permission_annotate) as process_permission_annotate,
       bool_and(accesses.process_permission_publish)  as process_permission_publish
from (select proc.id                                                            as process_definition_id,
             mem.user_id                                                        as user_id,

             mem.process_permission_create or rac.process_permission_create     as process_permission_create,
             mem.process_permission_read or rac.process_permission_read         as process_permission_read,
             mem.process_permission_edit or rac.process_permission_edit         as process_permission_edit,
             mem.process_permission_delete or rac.process_permission_delete     as process_permission_delete,
             mem.process_permission_annotate or rac.process_permission_annotate as process_permission_annotate,
             mem.process_permission_publish or rac.process_permission_publish   as process_permission_publish
      from process_definitions as proc
               join resource_access_controls rac on rac.target_process_id = proc.id
               join v_memberships_with_permissions mem on mem.department_id = rac.source_department_id or
                                                          mem.team_id = rac.source_team_id
      union
      select proc.id     as process_definition_id,
             mem.user_id as user_id,

             true        as process_permission_create,
             true        as process_permission_read,
             true        as process_permission_edit,
             true        as process_permission_delete,
             true        as process_permission_annotate,
             true        as process_permission_publish
      from process_definitions as proc
               join v_department_memberships_with_permissions mem on
          mem.department_id = proc.department_id) as accesses
group by process_definition_id, user_id;

-- create view for accessible process definitions
create view v_process_definitions_with_permissions as
select proc.*,

       perms.user_id                     as user_id,

       perms.process_permission_create   as process_permission_create,
       perms.process_permission_read     as process_permission_read,
       perms.process_permission_edit     as process_permission_edit,
       perms.process_permission_delete   as process_permission_delete,
       perms.process_permission_annotate as process_permission_annotate,
       perms.process_permission_publish  as process_permission_publish
from process_definitions proc
         join v_user_process_definition_permissions perms on proc.id = perms.process_definition_id;