insert into domain_roles (id,
                          name,
                          description,
                          permissions)
values (1,
        'Domänenadministration',
        'Rolle für Domänenadministratoren mit erweiterten Rechten',
        array [
            'department.read',
            'department.update',

            'department_membership.create',
            'department_membership.read',
            'department_membership.update',
            'department_membership.delete',

            'team.read',
            'team.update',

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
            'process_instance.edit_task'
            ]),
       (2,
        'Sachbearbeitung',
        'Rolle für Sachbearbeiter mit eingeschränkten Rechten',
        array [
            'department.read',
            'department_membership.read',
            'team.read',
            'team_membership.read',
            'domain_resource_permission.read',
            'process_definition.read',

            'process_instance.trigger',
            'process_instance.read',
            'process_instance.pause_resume',
            'process_instance.edit_data',
            'process_instance.communication.internal',
            'process_instance.communication.external',
            'process_instance.edit_task'
            ]),
       (3,
        'Modellierung',
        'Rolle für Modellierer mit Rechten zur Prozessdefinition',
        array [
            'department.read',
            'department_membership.read',
            'team.read',
            'team_membership.read',
            'domain_resource_permission.read',

            'process_definition.create',
            'process_definition.read',
            'process_definition.update',
            'process_definition.publish.test',

            'process_instance.read',
            'process_instance.communication.internal'
            ]),
       (4,
        'Prüfung',
        'Rolle für Prüfer mit Rechten zur Prozessdefinition',
        array [
            'department.read',
            'department_membership.read',
            'team.read',
            'team_membership.read',
            'domain_resource_permission.read',

            'process_definition.read',
            'process_definition.audit',
            'process_definition.publish.test',

            'process_instance.read',
            'process_instance.communication.internal'
            ]),
       (5,
        'Prozessmanagement',
        'Rolle für Prozessmanager mit Rechten zur Prozessdefinition',
        array [
            'department.read',
            'department_membership.read',
            'team.read',
            'team_membership.read',
            'domain_resource_permission.read',

            'process_definition.read',
            'process_definition.delete',
            'process_definition.publish.test',
            'process_definition.publish.local',
            'process_definition.publish.store',

            'process_instance.read',
            'process_instance.communication.internal'
            ]),
       (6,
        'Betrachtung',
        'Rolle für Betrachter mit Rechten zur Prozessdefinition',
        array [
            'department.read',
            'department_membership.read',
            'team.read',
            'team_membership.read',
            'domain_resource_permission.read',

            'process_definition.read',

            'process_instance.read',
            'process_instance.communication.internal',
            'process_instance.communication.external'
            ])
on conflict (id) do update
    set name        = excluded.name,
        description = excluded.description,
        permissions = excluded.permissions;

-- fix id sequence for domain_roles table
select setval('domain_roles_id_seq',
              (select max(id) from domain_roles));