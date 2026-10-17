package de.aivot.prosuna.backend.seeding.seeds;

import de.aivot.prosuna.backend.department.permissions.DepartmentPermissionProvider;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.permissions.DomainResourcePermissionProvider;
import de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.seeding.models.SeedProvider;
import de.aivot.prosuna.backend.teams.permissions.TeamPermissionProvider;
import de.aivot.prosuna.backend.userRoles.entities.UserRoleEntity;
import de.aivot.prosuna.backend.userRoles.repositories.UserRoleRepository;
import de.aivot.prosuna.backend.userRoles.services.UserRoleService;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DomainRolesSeed implements SeedProvider {
    private final UserRoleService userRoleService;
    private final UserRoleRepository userRoleRepository;

    public DomainRolesSeed(UserRoleService userRoleService,
                           UserRoleRepository userRoleRepository) {
        this.userRoleService = userRoleService;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    public boolean shouldSeed() {
        // Domain roles are editable defaults, so they are only created for an installation without any domain role.
        return userRoleRepository.count() == 0;
    }

    @Override
    public void seed() throws ResponseException {
        userRoleService.create(new UserRoleEntity()
                .setName("Domänenadministration")
                .setDescription("Rolle für Domänenadministratoren mit erweiterten Rechten")
                .setPermissions(List.of(
                        DepartmentPermissionProvider.DEPARTMENT_READ,
                        DepartmentPermissionProvider.DEPARTMENT_UPDATE,

                        DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_CREATE,
                        DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_READ,
                        DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_UPDATE,
                        DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_DELETE,

                        TeamPermissionProvider.TEAM_READ,
                        TeamPermissionProvider.TEAM_UPDATE,

                        TeamPermissionProvider.TEAM_MEMBERSHIP_CREATE,
                        TeamPermissionProvider.TEAM_MEMBERSHIP_READ,
                        TeamPermissionProvider.TEAM_MEMBERSHIP_UPDATE,
                        TeamPermissionProvider.TEAM_MEMBERSHIP_DELETE,

                        DomainResourcePermissionProvider.DOMAIN_RESOURCE_PERMISSION_CREATE,
                        DomainResourcePermissionProvider.DOMAIN_RESOURCE_PERMISSION_READ,
                        DomainResourcePermissionProvider.DOMAIN_RESOURCE_PERMISSION_UPDATE,
                        DomainResourcePermissionProvider.DOMAIN_RESOURCE_PERMISSION_DELETE,

                        ProcessPermissionProvider.PROCESS_DEFINITION_CREATE,
                        ProcessPermissionProvider.PROCESS_DEFINITION_READ,
                        ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE,
                        ProcessPermissionProvider.PROCESS_DEFINITION_DELETE,
                        ProcessPermissionProvider.PROCESS_DEFINITION_AUDIT,
                        ProcessPermissionProvider.PROCESS_DEFINITION_PUBLISH_TEST,
                        ProcessPermissionProvider.PROCESS_DEFINITION_PUBLISH_LOCAL,
                        ProcessPermissionProvider.PROCESS_DEFINITION_PUBLISH_MARKETPLACE,

                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_TRIGGER,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_UPDATE,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_DELETE,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_PAUSE_RESUME,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_REASSIGN,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_COMMUNICATION_INTERNAL,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_COMMUNICATION_EXTERNAL,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_EDIT_TASK
                )));

        userRoleService.create(new UserRoleEntity()
                .setName("Sachbearbeitung")
                .setDescription("Rolle für Sachbearbeiter mit eingeschränkten Rechten")
                .setPermissions(List.of(
                        DepartmentPermissionProvider.DEPARTMENT_READ,
                        DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_READ,
                        TeamPermissionProvider.TEAM_READ,
                        TeamPermissionProvider.TEAM_MEMBERSHIP_READ,
                        DomainResourcePermissionProvider.DOMAIN_RESOURCE_PERMISSION_READ,
                        ProcessPermissionProvider.PROCESS_DEFINITION_READ,

                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_TRIGGER,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_PAUSE_RESUME,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_COMMUNICATION_INTERNAL,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_COMMUNICATION_EXTERNAL,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_EDIT_TASK
                )));

        userRoleService.create(new UserRoleEntity()
                .setName("Modellierung")
                .setDescription("Rolle für Modellierer mit Rechten zur Prozessdefinition")
                .setPermissions(List.of(
                        DepartmentPermissionProvider.DEPARTMENT_READ,
                        DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_READ,
                        TeamPermissionProvider.TEAM_READ,
                        TeamPermissionProvider.TEAM_MEMBERSHIP_READ,
                        DomainResourcePermissionProvider.DOMAIN_RESOURCE_PERMISSION_READ,

                        ProcessPermissionProvider.PROCESS_DEFINITION_CREATE,
                        ProcessPermissionProvider.PROCESS_DEFINITION_READ,
                        ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE,
                        ProcessPermissionProvider.PROCESS_DEFINITION_PUBLISH_TEST,

                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_COMMUNICATION_INTERNAL
                )));

        userRoleService.create(new UserRoleEntity()
                .setName("Prüfung")
                .setDescription("Rolle für Prüfer mit Rechten zur Prozessdefinition")
                .setPermissions(List.of(
                        DepartmentPermissionProvider.DEPARTMENT_READ,
                        DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_READ,
                        TeamPermissionProvider.TEAM_READ,
                        TeamPermissionProvider.TEAM_MEMBERSHIP_READ,
                        DomainResourcePermissionProvider.DOMAIN_RESOURCE_PERMISSION_READ,

                        ProcessPermissionProvider.PROCESS_DEFINITION_READ,
                        ProcessPermissionProvider.PROCESS_DEFINITION_AUDIT,
                        ProcessPermissionProvider.PROCESS_DEFINITION_PUBLISH_TEST,

                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_COMMUNICATION_INTERNAL
                )));

        userRoleService.create(new UserRoleEntity()
                .setName("Prozessmanagement")
                .setDescription("Rolle für Prozessmanager mit Rechten zur Prozessdefinition")
                .setPermissions(List.of(
                        DepartmentPermissionProvider.DEPARTMENT_READ,
                        DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_READ,
                        TeamPermissionProvider.TEAM_READ,
                        TeamPermissionProvider.TEAM_MEMBERSHIP_READ,
                        DomainResourcePermissionProvider.DOMAIN_RESOURCE_PERMISSION_READ,

                        ProcessPermissionProvider.PROCESS_DEFINITION_READ,
                        ProcessPermissionProvider.PROCESS_DEFINITION_DELETE,
                        ProcessPermissionProvider.PROCESS_DEFINITION_PUBLISH_TEST,
                        ProcessPermissionProvider.PROCESS_DEFINITION_PUBLISH_LOCAL,
                        ProcessPermissionProvider.PROCESS_DEFINITION_PUBLISH_MARKETPLACE,

                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_COMMUNICATION_INTERNAL
                )));

        userRoleService.create(new UserRoleEntity()
                .setName("Betrachtung")
                .setDescription("Rolle für Betrachter mit Rechten zur Prozessdefinition")
                .setPermissions(List.of(
                        DepartmentPermissionProvider.DEPARTMENT_READ,
                        DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_READ,
                        TeamPermissionProvider.TEAM_READ,
                        TeamPermissionProvider.TEAM_MEMBERSHIP_READ,
                        DomainResourcePermissionProvider.DOMAIN_RESOURCE_PERMISSION_READ,

                        ProcessPermissionProvider.PROCESS_DEFINITION_READ,

                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_COMMUNICATION_INTERNAL,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_COMMUNICATION_EXTERNAL
                )));
    }
}
