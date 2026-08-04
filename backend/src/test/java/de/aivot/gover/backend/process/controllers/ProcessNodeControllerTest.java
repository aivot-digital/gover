package de.aivot.gover.backend.process.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.gover.backend.audit.services.AuditService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.permissions.services.PermissionService;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.gover.backend.process.repositories.ProcessNodeRepository;
import de.aivot.gover.backend.process.repositories.ProcessTestClaimRepository;
import de.aivot.gover.backend.process.services.ProcessNodeDefinitionService;
import de.aivot.gover.backend.process.services.ProcessNodeExportService;
import de.aivot.gover.backend.process.services.ProcessNodeService;
import de.aivot.gover.backend.process.services.ProcessService;
import de.aivot.gover.backend.process.services.ProcessVersionService;
import de.aivot.gover.backend.user.entities.UserEntity;
import de.aivot.gover.backend.user.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessNodeControllerTest {
    @Test
    void updatePreservesExistingProcessScope() throws ResponseException {
        var jwt = mock(Jwt.class);
        var user = mock(UserEntity.class);
        when(user.getId()).thenReturn("user-1");
        when(user.getFullName()).thenReturn("Test User");
        var userService = mock(UserService.class);
        when(userService.fromJWT(jwt)).thenReturn(Optional.of(user));

        var existingNode = new ProcessNodeEntity()
                .setId(7)
                .setProcessId(12)
                .setProcessVersion(5)
                .setDataKey("existing-key");
        var processNodeService = mock(ProcessNodeService.class);
        when(processNodeService.retrieve(7)).thenReturn(Optional.of(existingNode));
        when(processNodeService.update(eq(7), any(ProcessNodeEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        var processNodeRepository = mock(ProcessNodeRepository.class);
        var permissionService = mock(PermissionService.class);
        var objectMapper = mock(ObjectMapper.class);
        when(objectMapper.convertValue(any(), eq(Map.class))).thenReturn(Map.of());
        var controller = new ProcessNodeController(
                mock(AuditService.class, RETURNS_DEEP_STUBS),
                userService,
                processNodeService,
                mock(ProcessService.class),
                permissionService,
                mock(ProcessNodeDefinitionService.class),
                mock(ProcessNodeExportService.class),
                mock(ProcessVersionService.class),
                mock(ProcessTestClaimRepository.class),
                objectMapper,
                processNodeRepository
        );
        var submittedNode = new ProcessNodeEntity()
                .setId(99)
                .setProcessId(42)
                .setProcessVersion(9)
                .setDataKey("updated-key");

        var result = controller.update(jwt, 7, submittedNode, null, null);

        assertEquals(7, result.getId());
        assertEquals(12, result.getProcessId());
        assertEquals(5, result.getProcessVersion());
        verify(permissionService).requireProcessPermission(
                "user-1",
                12,
                ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE
        );
        verify(processNodeRepository).existsByDataKeyAndIdIsNotAndProcessIdAndProcessVersion(
                "updated-key",
                7,
                12,
                5
        );
        verify(processNodeService).update(7, submittedNode);
    }
}
