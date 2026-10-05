package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.department.entities.DepartmentEntity;
import de.aivot.prosuna.backend.department.repositories.DepartmentRepository;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAccessControlEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAccessControlPresetEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessRepository;
import de.aivot.prosuna.backend.process.services.PotentialProcessInstanceAccessService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAccessAuditDescriptionService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAccessControlPresetService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAccessControlService;
import de.aivot.prosuna.backend.teams.entities.TeamEntity;
import de.aivot.prosuna.backend.teams.repositories.TeamRepository;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ProcessInstanceAccessAuditMessageTest {
    private final ProcessRepository processes = mock(ProcessRepository.class);
    private final ProcessInstanceRepository instances = mock(ProcessInstanceRepository.class);
    private final DepartmentRepository departments = mock(DepartmentRepository.class);
    private final TeamRepository teams = mock(TeamRepository.class);
    private final ProcessInstanceAccessAuditDescriptionService descriptions =
            new ProcessInstanceAccessAuditDescriptionService(processes, instances, departments, teams);
    private final AuditService audit = mock(AuditService.class, RETURNS_DEEP_STUBS);
    private final UserEntity actor = mock(UserEntity.class);

    @Test
    void presetsDescribeNewInstancesOfTheProcessVersionAndTheRecipient() {
        when(actor.getFullName()).thenReturn("Alex Beispiel");
        when(processes.findById(5)).thenReturn(Optional.of(new ProcessEntity().setInternalTitle("Bauantrag")));
        when(departments.findById(7)).thenReturn(Optional.of(new DepartmentEntity().setName("Bauamt")));
        var controller = new ProcessInstanceAccessControlPresetController(audit, mock(UserService.class),
                mock(ProcessInstanceAccessControlPresetService.class), mock(PermissionService.class), descriptions);
        var entry = new ProcessInstanceAccessControlPresetEntity().setId(2).setTargetProcessId(5)
                .setTargetProcessVersion(3).setSourceDepartmentId(7);
        var subject = "„Alex Beispiel“ hat die Standardberechtigungen für neue Vorgänge des Prozesses „Bauantrag“ (Version 3) für die Organisationseinheit „Bauamt“ ";

        assertEquals(subject + "hinzugefügt.", controller.buildCreateAuditMessage(actor, entry));
        assertEquals(subject + "geändert.", controller.buildUpdateAuditMessage(actor, 2, entry));
        assertEquals(subject + "entfernt.", controller.buildDeleteAuditMessage(actor, 2, entry));
    }

    @Test
    void instanceChangesDescribeTheCaseNumberAndTeam() {
        when(actor.getFullName()).thenReturn("Alex Beispiel");
        when(instances.findById(17L)).thenReturn(Optional.of(new ProcessInstanceEntity().setCaseNumber("VG-2026-0042")));
        when(teams.findById(8)).thenReturn(Optional.of(new TeamEntity().setName("Prüfteam")));
        var controller = new ProcessInstanceAccessControlController(audit, mock(UserService.class),
                mock(ProcessInstanceAccessControlService.class), mock(PotentialProcessInstanceAccessService.class),
                mock(PermissionService.class), descriptions);
        var entry = new ProcessInstanceAccessControlEntity().setId(2).setTargetProcessInstanceId(17L).setSourceTeamId(8);
        var subject = "„Alex Beispiel“ hat die Berechtigungen am Vorgang „VG-2026-0042“ für das Team „Prüfteam“ ";

        assertEquals(subject + "hinzugefügt.", controller.buildCreateAuditMessage(actor, entry));
        assertEquals(subject + "geändert.", controller.buildUpdateAuditMessage(actor, 2, entry));
        assertEquals(subject + "entfernt.", controller.buildDeleteAuditMessage(actor, 2, entry));
    }

    @Test
    void missingNamesFallBackToIdentifiableReferences() {
        var preset = new ProcessInstanceAccessControlPresetEntity().setTargetProcessId(5)
                .setTargetProcessVersion(3).setSourceTeamId(8);
        var access = new ProcessInstanceAccessControlEntity().setTargetProcessInstanceId(17L).setSourceDepartmentId(7);

        assertEquals("die Standardberechtigungen für neue Vorgänge des Prozesses mit der ID 5 (Version 3) für das Team mit der ID 8",
                descriptions.describePreset(preset));
        assertEquals("die Berechtigungen am Vorgang mit der ID 17 für die Organisationseinheit mit der ID 7",
                descriptions.describeAccess(access));
    }
}
