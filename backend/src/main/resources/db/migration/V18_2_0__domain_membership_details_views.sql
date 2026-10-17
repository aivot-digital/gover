-- create a view of all permissions a user has in a specific domain, based on their department memberships and team memberships.
-- the permissions include the domain role permissions assigned to the department and team memberships as well as the system role permissions of the users.
create view v_user_domain_permissions as

select dmp.user_id                          as user_id,
       dmp.department_id                    as department_id,
       null                                 as team_id,
       dmp.is_direct_member                 as is_direct_member,
       dmp.is_indirect_member               as is_indirect_member,
       dmp.direct_system_role_permissions   as direct_system_role_permissions,
       dmp.indirect_system_role_permissions as indirect_system_role_permissions,
       dmp.system_role_permissions          as system_role_permissions,
       dmp.system_role_names                as system_role_names,
       dmp.system_role_ids                  as system_role_ids,
       dmp.direct_domain_role_permissions   as direct_domain_role_permissions,
       dmp.indirect_domain_role_permissions as indirect_domain_role_permissions,
       dmp.domain_role_permissions          as domain_role_permissions,
       dmp.domain_role_names                as domain_role_names,
       dmp.domain_role_ids                  as domain_role_ids,
       dmp.direct_permissions               as direct_permissions,
       dmp.indirect_permissions             as indirect_permissions,
       dmp.permissions                      as permissions,
       dmp.deputy_for_user_ids              as deputy_for_user_ids
from v_user_department_permissions as dmp

union

select tmp.user_id                          as user_id,
       null                                 as department_id,
       tmp.team_id                          as team_id,
       tmp.is_direct_member                 as is_direct_member,
       tmp.is_indirect_member               as is_indirect_member,
       tmp.direct_system_role_permissions   as direct_system_role_permissions,
       tmp.indirect_system_role_permissions as indirect_system_role_permissions,
       tmp.system_role_permissions          as system_role_permissions,
       tmp.system_role_names                as system_role_names,
       tmp.system_role_ids                  as system_role_ids,
       tmp.direct_domain_role_permissions   as direct_domain_role_permissions,
       tmp.indirect_domain_role_permissions as indirect_domain_role_permissions,
       tmp.domain_role_permissions          as domain_role_permissions,
       tmp.domain_role_names                as domain_role_names,
       tmp.domain_role_ids                  as domain_role_ids,
       tmp.direct_permissions               as direct_permissions,
       tmp.indirect_permissions             as indirect_permissions,
       tmp.permissions                      as permissions,
       tmp.deputy_for_user_ids              as deputy_for_user_ids
from v_user_team_permissions as tmp
