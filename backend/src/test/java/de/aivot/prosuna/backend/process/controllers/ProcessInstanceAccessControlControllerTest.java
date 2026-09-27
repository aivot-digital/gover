package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.filters.ProcessInstanceAccessControlFilter;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.services.PotentialProcessInstanceAccessService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAccessControlService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAccessAuditDescriptionService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.aivot.prosuna.backend.process.entities.ProcessInstanceAccessControlEntity;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessInstanceAccessControlControllerTest {
    private final Jwt jwt = mock(Jwt.class);
    private final UserService userService = mock(UserService.class);
    private final ProcessInstanceAccessControlService accessControlService = mock(ProcessInstanceAccessControlService.class);
    private final PermissionService permissionService = mock(PermissionService.class);
    private final UserEntity user = mock(UserEntity.class);

    private ProcessInstanceAccessControlController controller;

    @BeforeEach
    void setUp() throws ResponseException {
        when(user.getId()).thenReturn("user-1");
        when(userService.fromJWT(jwt)).thenReturn(Optional.of(user));
        controller = new ProcessInstanceAccessControlController(
                mock(AuditService.class, RETURNS_DEEP_STUBS),
                userService,
                accessControlService,
                mock(PotentialProcessInstanceAccessService.class),
                permissionService,
                mock(ProcessInstanceAccessAuditDescriptionService.class)
        );
    }

    @Test
    void listPreservesProcessInstanceIdsOutsideIntegerRange() throws ResponseException {
        var pageable = PageRequest.of(0, 20);
        var filter = ProcessInstanceAccessControlFilter.create();
        var processInstanceId = (long) Integer.MAX_VALUE + 1;
        when(permissionService.getProcessInstancesWithPermission(
                "user-1",
                ProcessPermissionProvider.PROCESS_INSTANCE_READ
        )).thenReturn(List.of(processInstanceId));
        when(accessControlService.list(pageable, filter)).thenReturn(Page.empty(pageable));

        controller.list(jwt, pageable, filter);

        assertEquals(List.of(processInstanceId), filter.getTargetProcessInstanceIds());
        verify(accessControlService).list(pageable, filter);
    }

    @Test
    void listWithSystemReadDoesNotRestrictTargets() throws ResponseException {
        var pageable = PageRequest.of(0, 20);
        var filter = ProcessInstanceAccessControlFilter.create();
        when(permissionService.hasSystemPermission("user-1", ProcessPermissionProvider.PROCESS_INSTANCE_READ)).thenReturn(true);
        when(accessControlService.list(pageable, filter)).thenReturn(Page.empty(pageable));
        controller.list(jwt, pageable, filter);
        assertNull(filter.getTargetProcessInstanceIds());
        verify(permissionService, never()).getProcessInstancesWithPermission(anyString(), anyString());
    }

    @Test
    void listRestrictsRequestedIdsToReadableInstances() throws ResponseException {
        var pageable = PageRequest.of(0, 20);
        var filter = ProcessInstanceAccessControlFilter.create();
        filter.setTargetProcessInstanceIds(List.of(17L, 18L));
        when(permissionService.getProcessInstancesWithPermission("user-1", ProcessPermissionProvider.PROCESS_INSTANCE_READ)).thenReturn(List.of(17L));
        when(accessControlService.list(pageable, filter)).thenReturn(Page.empty(pageable));
        controller.list(jwt, pageable, filter);
        assertEquals(List.of(17L), filter.getTargetProcessInstanceIds());
    }

    @Test
    void listRejectsUnreadableSpecificInstance() throws ResponseException {
        var filter = ProcessInstanceAccessControlFilter.create();
        filter.setTargetProcessInstanceId(18L);
        doThrow(ResponseException.forbidden()).when(permissionService).requireProcessInstancePermission(
                "user-1", 18L, ProcessPermissionProvider.PROCESS_INSTANCE_READ);
        assertThrows(ResponseException.class, () -> controller.list(jwt, PageRequest.of(0, 20), filter));
        verifyNoInteractions(accessControlService);
    }

    @Test
    void readPermissionDoesNotAllowMutations() throws ResponseException {
        var access = new ProcessInstanceAccessControlEntity().setId(5).setTargetProcessInstanceId(17L);
        when(accessControlService.retrieve(5)).thenReturn(Optional.of(access));
        doThrow(ResponseException.forbidden()).when(permissionService).requireProcessInstancePermission(
                "user-1", 17L, ProcessPermissionProvider.PROCESS_INSTANCE_UPDATE);
        controller.checkRetrievePermissions(user, 5);
        verify(permissionService).requireProcessInstancePermission("user-1", 17L, ProcessPermissionProvider.PROCESS_INSTANCE_READ);
        assertThrows(ResponseException.class, () -> controller.checkCreatePermissions(user, access));
        assertThrows(ResponseException.class, () -> controller.checkUpdatePermission(user, 5));
        assertThrows(ResponseException.class, () -> controller.checkDeletePermission(user, 5));
    }

    @Test
    void mutationsRequireTheStoredTargetInstance() throws ResponseException {
        var access = new ProcessInstanceAccessControlEntity().setId(5).setTargetProcessInstanceId(17L);
        when(accessControlService.retrieve(5)).thenReturn(Optional.of(access));
        controller.checkCreatePermissions(user, access);
        controller.checkUpdatePermission(user, 5);
        controller.checkDeletePermission(user, 5);
        verify(permissionService, times(3)).requireProcessInstancePermission("user-1", 17L, ProcessPermissionProvider.PROCESS_INSTANCE_UPDATE);
    }

    @Test
    void replacesRulesThroughTheAuthenticatedServiceWithoutSeparateCrudCalls() throws Exception {
        var rules = List.of(new de.aivot.prosuna.backend.process.dtos.ProcessInstanceAccessRuleDTO(null, 8, List.of(ProcessPermissionProvider.PROCESS_INSTANCE_READ)));
        when(accessControlService.replace(user, 17L, rules)).thenReturn(List.of());
        assertEquals(List.of(), controller.replace(jwt, 17L, rules));
        verify(accessControlService).replace(user, 17L, rules);
        verifyNoMoreInteractions(accessControlService);
    }

    @Test
    void replacementRequiresAuthentication() throws Exception {
        when(userService.fromJWT(jwt)).thenReturn(Optional.empty());
        assertThrows(ResponseException.class, () -> controller.replace(jwt, 17L, List.of()));
        verifyNoInteractions(accessControlService);
    }

}
