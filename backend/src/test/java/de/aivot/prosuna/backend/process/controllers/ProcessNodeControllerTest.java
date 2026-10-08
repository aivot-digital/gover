package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.config.services.SystemConfigService;
import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntityId;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodePortResolution;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.repositories.ProcessNodeRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessTestClaimRepository;
import de.aivot.prosuna.backend.process.services.ProcessNodeDefinitionService;
import de.aivot.prosuna.backend.process.services.ProcessNodeExportService;
import de.aivot.prosuna.backend.process.services.ProcessNodeService;
import de.aivot.prosuna.backend.process.services.ProcessService;
import de.aivot.prosuna.backend.process.services.ProcessVersionService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import de.aivot.prosuna.backend.system.properties.BuildProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jwt.Jwt;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessNodeControllerTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void nodeExportAndImportPreserveDeadlineStrictness(boolean strict) throws Exception {
        var source = new ProcessNodeEntity().setId(7).setProcessId(12).setProcessVersion(5)
                .setDataKey("source").setProcessNodeDefinitionKey("test.node").setProcessNodeDefinitionVersion(1)
                .setConfiguration(new AuthoredElementValues()).setOutputMappings(Map.of())
                .setTimeLimitDays(3).setTimeLimitStrict(strict);
        var nodes = mock(ProcessNodeService.class);
        when(nodes.retrieve(7)).thenReturn(Optional.of(source));
        when(nodes.create(any())).thenAnswer(invocation -> ((ProcessNodeEntity) invocation.getArgument(0)).setId(8));
        when(nodes.resolvePorts(any())).thenReturn(new ProcessNodePortResolution(List.of(), List.of()));
        var definitions = mock(ProcessNodeDefinitionService.class);
        var definition = mock(ProcessNodeDefinition.class);
        when(definitions.getProcessNodeDefinition("test.node", 1)).thenReturn(Optional.of(definition));
        when(definition.cleanConfigurationForExport(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(definition.prefillConfigurationOnImport(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var versions = mock(ProcessVersionService.class);
        when(versions.retrieve(ProcessVersionEntityId.of(20, 1))).thenReturn(Optional.of(new ProcessVersionEntity()));
        var users = mock(UserService.class);
        when(users.fromJWT(null)).thenReturn(Optional.of(new UserEntity().setId("staff").setFullName("Anna Beispiel")));
        var exportService = new ProcessNodeExportService(mock(ProcessService.class), nodes, definitions,
                new BuildProperties(), mock(SystemConfigService.class, RETURNS_DEEP_STUBS));
        var mapper = JsonMapperFactory.getInstance();
        var exported = exportService.export(7);
        var imported = mapper.readValue(mapper.writeValueAsString(exported), ProcessNodeExportService.ProcessNodeExport.class);
        var controller = new ProcessNodeController(mock(AuditService.class, RETURNS_DEEP_STUBS), users, nodes,
                mock(ProcessService.class), mock(PermissionService.class), definitions, exportService, versions,
                mock(ProcessTestClaimRepository.class), mapper, mock(ProcessNodeRepository.class));

        var result = controller.importNode(null, 20, 1, imported);

        assertEquals(strict, result.timeLimitStrict());
        assertEquals(3, result.timeLimitDays());
        assertEquals(20, result.processId());
        assertEquals(1, result.processVersion());
    }

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
        when(processNodeService.resolvePorts(any())).thenReturn(new de.aivot.prosuna.backend.process.models.ProcessNodePortResolution(java.util.List.of(), java.util.List.of()));
        when(processNodeService.retrieve(7)).thenReturn(Optional.of(existingNode));
        when(processNodeService.update(eq(7), any(ProcessNodeEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        var processNodeRepository = mock(ProcessNodeRepository.class);
        var permissionService = mock(PermissionService.class);
        var objectMapper = mock(JsonMapper.class);
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

        assertEquals(7, result.id());
        assertEquals(12, result.processId());
        assertEquals(5, result.processVersion());
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
