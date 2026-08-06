package de.aivot.gover.backend.process.controllers;

import de.aivot.gover.backend.audit.services.AuditService;
import de.aivot.gover.backend.department.services.DepartmentService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.permissions.services.PermissionService;
import de.aivot.gover.backend.process.entities.ProcessEntity;
import de.aivot.gover.backend.process.entities.ProcessVersionEntity;
import de.aivot.gover.backend.process.entities.ProcessVersionEntityId;
import de.aivot.gover.backend.process.enums.ProcessVersionStatus;
import de.aivot.gover.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.gover.backend.process.services.ProcessService;
import de.aivot.gover.backend.process.services.ProcessVersionService;
import de.aivot.gover.backend.user.entities.UserEntity;
import de.aivot.gover.backend.user.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessVersionControllerTest {
    @Test
    void createAlwaysCreatesDraftWithoutPublicationTimestamps() throws ResponseException {
        var jwt = mock(Jwt.class);
        var user = user("user-1");
        var userService = mock(UserService.class);
        when(userService.fromJWT(jwt)).thenReturn(Optional.of(user));

        var process = mock(ProcessEntity.class);
        when(process.getId()).thenReturn(12);
        var processService = mock(ProcessService.class);
        when(processService.retrieve(12)).thenReturn(Optional.of(process));

        var processVersionService = mock(ProcessVersionService.class);
        when(processVersionService.create(any(ProcessVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        var permissionService = mock(PermissionService.class);
        var controller = controller(userService, processVersionService, processService, permissionService);
        var submittedVersion = new ProcessVersionEntity()
                .setProcessId(12)
                .setProcessVersion(99)
                .setStatus(ProcessVersionStatus.Published)
                .setPublicTitle("Published without permission")
                .setPublished(Instant.parse("2026-01-01T00:00:00Z"))
                .setRevoked(Instant.parse("2026-02-01T00:00:00Z"));

        var result = controller.create(jwt, submittedVersion);

        assertEquals(ProcessVersionStatus.Drafted, result.getStatus());
        assertNull(result.getPublished());
        assertNull(result.getRevoked());
        verify(permissionService).requireProcessPermission(
                "user-1",
                12,
                ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE
        );
        verify(processVersionService).create(submittedVersion);
    }

    @Test
    void updatePreservesExistingStatus() throws ResponseException {
        var jwt = mock(Jwt.class);
        var user = user("user-1");
        var userService = mock(UserService.class);
        when(userService.fromJWT(jwt)).thenReturn(Optional.of(user));

        var existingVersion = new ProcessVersionEntity()
                .setProcessId(12)
                .setProcessVersion(5)
                .setStatus(ProcessVersionStatus.Drafted)
                .setPublicTitle("Existing version");
        var versionId = ProcessVersionEntityId.of(12, 5);
        var processVersionService = mock(ProcessVersionService.class);
        when(processVersionService.retrieve(versionId)).thenReturn(Optional.of(existingVersion));
        when(processVersionService.update(any(ProcessVersionEntityId.class), any(ProcessVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        var permissionService = mock(PermissionService.class);
        var controller = controller(
                userService,
                processVersionService,
                mock(ProcessService.class),
                permissionService
        );
        var submittedVersion = new ProcessVersionEntity()
                .setProcessId(99)
                .setProcessVersion(99)
                .setStatus(ProcessVersionStatus.Published)
                .setPublicTitle("Updated version");

        var result = controller.update(jwt, 12, 5, submittedVersion);

        assertEquals(12, result.getProcessId());
        assertEquals(5, result.getProcessVersion());
        assertEquals(ProcessVersionStatus.Drafted, result.getStatus());
        verify(permissionService).requireProcessPermission(
                "user-1",
                12,
                ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE
        );
        verify(processVersionService).update(versionId, submittedVersion);
    }

    private static ProcessVersionController controller(UserService userService,
                                                       ProcessVersionService processVersionService,
                                                       ProcessService processService,
                                                       PermissionService permissionService) {
        return new ProcessVersionController(
                mock(AuditService.class, RETURNS_DEEP_STUBS),
                userService,
                processVersionService,
                mock(DepartmentService.class),
                processService,
                permissionService
        );
    }

    private static UserEntity user(String id) {
        var user = mock(UserEntity.class);
        when(user.getId()).thenReturn(id);
        when(user.getFullName()).thenReturn("Test User");
        return user;
    }
}
