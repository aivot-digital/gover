insert into system_roles (id, name, description, permissions)
values (1,
        'Superadministrator:in',
        'Hat uneingeschränkten Zugriff auf alle Funktionen und Einstellungen des Systems.',
        array [
            'asset.create',
            'asset.read',
            'asset.update',
            'asset.delete',

            'object_schema.create',
            'object_schema.read',
            'object_schema.update',
            'object_schema.delete',

            'object_item.create',
            'object_item.read',
            'object_item.update',
            'object_item.delete',

            'department.create',
            'department.read',
            'department.update',
            'department.delete',

            'identity_provider.create',
            'identity_provider.read',
            'identity_provider.update',
            'identity_provider.delete',

            'payment_provider.create',
            'payment_provider.read',
            'payment_provider.update',
            'payment_provider.delete',

            'preset.create',
            'preset.read',
            'preset.update',
            'preset.delete',
            'preset.publish.local',
            'preset.publish.store',

            'secret.create',
            'secret.read',
            'secret.update',
            'secret.delete',

            'system_config.create',
            'system_config.read',
            'system_config.update',
            'system_config.delete',

            'system_role.create',
            'system_role.read',
            'system_role.update',
            'system_role.delete',

            'domain_role.create',
            'domain_role.read',
            'domain_role.update',
            'domain_role.delete',

            'team.create',
            'team.read',
            'team.update',
            'team.delete',

            'theme.create',
            'theme.read',
            'theme.update',
            'theme.delete',

            'user_config.create',
            'user_config.read',
            'user_config.update',
            'user_config.delete',

            'user.create',
            'user.read',
            'user.update',
            'user.delete',

            'plugin.create',
            'plugin.read',
            'plugin.update',
            'plugin.delete',

            'deputy.create',
            'deputy.read',
            'deputy.update',
            'deputy.delete',

            'department_membership.create',
            'department_membership.read',
            'department_membership.update',
            'department_membership.delete',

            'team_membership.create',
            'team_membership.read',
            'team_membership.update',
            'team_membership.delete',

            'domain_resource_permission.create',
            'domain_resource_permission.read',
            'domain_resource_permission.update',
            'domain_resource_permission.delete',

            'process_definition.create',
            'process_definition.read',
            'process_definition.update',
            'process_definition.delete',
            'process_definition.audit',
            'process_definition.publish.test',
            'process_definition.publish.local',
            'process_definition.publish.store',

            'process_instance.trigger',
            'process_instance.read',
            'process_instance.update',
            'process_instance.delete',
            'process_instance.pause_resume',
            'process_instance.edit_data',
            'process_instance.reassign',
            'process_instance.communication.internal',
            'process_instance.communication.external',
            'process_instance.edit_task',

            'storage_provider.read',
            'storage_provider.update',
            'storage_provider.create',
            'storage_provider.delete',

            'audit_log.read'
            ]),
       (2,
        'Systemadministrator:in',
        'Hat uneingeschränkten Zugriff auf alle Systemressourcen und Einstellungen.',
        array [
            'asset.create',
            'asset.read',
            'asset.update',
            'asset.delete',

            'object_schema.create',
            'object_schema.read',
            'object_schema.update',
            'object_schema.delete',

            'object_item.create',
            'object_item.read',
            'object_item.update',
            'object_item.delete',

            'department.create',
            'department.read',
            'department.update',
            'department.delete',

            'identity_provider.create',
            'identity_provider.read',
            'identity_provider.update',
            'identity_provider.delete',

            'payment_provider.create',
            'payment_provider.read',
            'payment_provider.update',
            'payment_provider.delete',

            'preset.create',
            'preset.read',
            'preset.update',
            'preset.delete',
            'preset.publish.local',
            'preset.publish.store',

            'secret.create',
            'secret.read',
            'secret.update',
            'secret.delete',

            'system_config.create',
            'system_config.read',
            'system_config.update',
            'system_config.delete',

            'system_role.create',
            'system_role.read',
            'system_role.update',
            'system_role.delete',

            'domain_role.create',
            'domain_role.read',
            'domain_role.update',
            'domain_role.delete',

            'team.create',
            'team.read',
            'team.update',
            'team.delete',

            'theme.create',
            'theme.read',
            'theme.update',
            'theme.delete',

            'user_config.create',
            'user_config.read',
            'user_config.update',
            'user_config.delete',

            'user.create',
            'user.read',
            'user.update',
            'user.delete',

            'plugin.create',
            'plugin.read',
            'plugin.update',
            'plugin.delete',

            'deputy.create',
            'deputy.read',
            'deputy.update',
            'deputy.delete',

            'department_membership.create',
            'department_membership.read',
            'department_membership.update',
            'department_membership.delete',

            'team_membership.create',
            'team_membership.read',
            'team_membership.update',
            'team_membership.delete',

            'domain_resource_permission.read',

            'storage_provider.read',
            'storage_provider.update',
            'storage_provider.create',
            'storage_provider.delete'
            ]),
       (3,
        'Mitarbeiter:in',
        'Hat nur Zugriff auf grundlegende Funktionen und Inhalte des Systems.',
        array [
            'asset.create',
            'asset.read',
            'asset.update',

            'object_schema.read',

            'object_item.create',
            'object_item.read',
            'object_item.update',

            'department.read',

            'identity_provider.read',

            'payment_provider.read',

            'preset.create',
            'preset.read',
            'preset.update',
            'preset.publish.local',

            'secret.read',

            'system_config.read',

            'system_role.read',

            'domain_role.read',

            'team.read',

            'theme.read',

            'user.read',

            'plugin.read',

            'deputy.read',

            'department_membership.read',

            'team_membership.read',

            'domain_resource_permission.read',

            'storage_provider.read'
            ])
on conflict (id) do update
    set name        = excluded.name,
        description = excluded.description,
        permissions = excluded.permissions;

-- set the default system role for automatically imported users
insert into system_configs (key,
                            value)
values ('users.default_system_role', '3')
on conflict (key) do update
    set value = excluded.value;

-- fix id sequence for system_roles
select setval('system_roles_id_seq',
              (select max(id) from system_roles));
