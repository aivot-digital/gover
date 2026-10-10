package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.ElementDerivationOptions;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.identity.services.IdentityService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeUiDefinitionDerivationRequest;
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
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessNodeControllerTest {
    private static final String UI_DEFINITION_FIELD_ID = "uiDefinition";
    private static final String IDENTITY_SESSION_ID = "identity-session";

    private final Jwt jwt = mock(Jwt.class);
    private final UserEntity user = mock(UserEntity.class);
    private final UserService userService = mock(UserService.class);
    private final ProcessNodeService processNodeService = mock(ProcessNodeService.class);
    private final PermissionService permissionService = mock(PermissionService.class);
    private final ProcessNodeDefinitionService processNodeDefinitionService = mock(ProcessNodeDefinitionService.class);
    private final ProcessNodeRepository processNodeRepository = mock(ProcessNodeRepository.class);
    private final IdentityService identityService = mock(IdentityService.class);
    private final JsonMapper objectMapper = mock(JsonMapper.class);

    @Test
    void updatePreservesExistingProcessScope() throws ResponseException {
        mockAuthenticatedUser();
        when(user.getFullName()).thenReturn("Test User");

        var existingNode = createNode();
        when(processNodeService.retrieve(7)).thenReturn(Optional.of(existingNode));
        when(processNodeService.update(eq(7), any(ProcessNodeEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(objectMapper.convertValue(any(), eq(Map.class))).thenReturn(Map.of());

        var controller = createController();
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

    @Test
    void deriveUiDefinitionRejectsUnsavedUiDefinitionWithoutUpdatePermission() throws ResponseException {
        mockAuthenticatedUser();
        when(processNodeService.retrieve(7)).thenReturn(Optional.of(createNode()));
        doThrow(ResponseException.forbidden()).when(permissionService).requireProcessPermission(
                "user-1",
                12,
                ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE
        );

        var controller = createController();
        var request = createRequest(new GroupLayoutElement());

        var exception = assertThrows(
                ResponseException.class,
                () -> controller.deriveUiDefinition(jwt, 7, request, IDENTITY_SESSION_ID)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verify(permissionService, never()).requireProcessPermission(
                "user-1",
                12,
                ProcessPermissionProvider.PROCESS_DEFINITION_READ
        );
        verify(processNodeService, never()).deriveUiDefinitionForAuthoring(
                any(), any(), any(), anyString(), any(), any(), any(), any()
        );
    }

    @Test
    void deriveUiDefinitionDerivesUnsavedUiDefinitionWithUpdatePermission() throws ResponseException {
        mockAuthenticatedUser();
        var node = createNode();
        when(processNodeService.retrieve(7)).thenReturn(Optional.of(node));
        var provider = mockProvider(node);
        var identities = new IdentityDataMap();
        when(identityService.getIdentityDataMap(IDENTITY_SESSION_ID, 7)).thenReturn(identities);
        var derivedData = DerivedRuntimeElementData.empty();
        var uiDefinition = new GroupLayoutElement();
        var request = createRequest(uiDefinition);
        when(processNodeService.deriveUiDefinitionForAuthoring(
                node,
                provider,
                user,
                UI_DEFINITION_FIELD_ID,
                uiDefinition,
                request.authoredElementValues(),
                request.derivationOptions(),
                identities
        )).thenReturn(derivedData);

        var result = createController().deriveUiDefinition(jwt, 7, request, IDENTITY_SESSION_ID);

        assertSame(derivedData, result);
        verify(permissionService).requireProcessPermission(
                "user-1",
                12,
                ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE
        );
    }

    @Test
    void deriveUiDefinitionDerivesStoredUiDefinitionWithReadPermission() throws ResponseException {
        mockAuthenticatedUser();
        var node = createNode();
        when(processNodeService.retrieve(7)).thenReturn(Optional.of(node));
        var provider = mockProvider(node);
        var identities = new IdentityDataMap();
        when(identityService.getIdentityDataMap(IDENTITY_SESSION_ID, 7)).thenReturn(identities);
        var request = createRequest(null);

        createController().deriveUiDefinition(jwt, 7, request, IDENTITY_SESSION_ID);

        verify(permissionService).requireProcessPermission(
                "user-1",
                12,
                ProcessPermissionProvider.PROCESS_DEFINITION_READ
        );
        verify(permissionService, never()).requireProcessPermission(
                "user-1",
                12,
                ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE
        );
        verify(processNodeService).deriveUiDefinitionForAuthoring(
                eq(node),
                eq(provider),
                eq(user),
                eq(UI_DEFINITION_FIELD_ID),
                isNull(),
                eq(request.authoredElementValues()),
                eq(request.derivationOptions()),
                eq(identities)
        );
    }

    private void mockAuthenticatedUser() throws ResponseException {
        when(user.getId()).thenReturn("user-1");
        when(userService.fromJWT(jwt)).thenReturn(Optional.of(user));
    }

    private ProcessNodeDefinition<?> mockProvider(ProcessNodeEntity node) {
        var provider = mock(ProcessNodeDefinition.class);
        doReturn(Optional.of(provider)).when(processNodeDefinitionService).getProcessNodeDefinition(node);
        return provider;
    }

    private ProcessNodeController createController() {
        return new ProcessNodeController(
                mock(AuditService.class, RETURNS_DEEP_STUBS),
                userService,
                processNodeService,
                mock(ProcessService.class),
                permissionService,
                processNodeDefinitionService,
                mock(ProcessNodeExportService.class),
                mock(ProcessVersionService.class),
                mock(ProcessTestClaimRepository.class),
                objectMapper,
                processNodeRepository,
                identityService
        );
    }

    private static ProcessNodeEntity createNode() {
        return new ProcessNodeEntity()
                .setId(7)
                .setProcessId(12)
                .setProcessVersion(5)
                .setDataKey("existing-key");
    }

    private static ProcessNodeUiDefinitionDerivationRequest createRequest(GroupLayoutElement uiDefinition) {
        return new ProcessNodeUiDefinitionDerivationRequest(
                UI_DEFINITION_FIELD_ID,
                uiDefinition,
                new AuthoredElementValues(),
                new ElementDerivationOptions()
        );
    }
}
