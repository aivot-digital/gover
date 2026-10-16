-- Complete the built-in superadministrator role without expanding other roles
-- or deriving grants from a role name. Preserve any existing custom grants.
update system_roles
set permissions = array_append(permissions, 'process_instance.edit_data')
where id = 1
  and not ('process_instance.edit_data' = any (permissions));

update system_roles
set permissions = array_append(permissions, 'permission_set.read')
where id = 1
  and not ('permission_set.read' = any (permissions));
