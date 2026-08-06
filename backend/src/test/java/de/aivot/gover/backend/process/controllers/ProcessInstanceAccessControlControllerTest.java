package de.aivot.gover.backend.process.controllers;

import de.aivot.gover.backend.audit.services.AuditService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.permissions.services.PermissionService;
import de.aivot.gover.backend.process.filters.ProcessInstanceAccessControlFilter;
import de.aivot.gover.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.gover.backend.process.services.PotentialProcessInstanceAccessService;
import de.aivot.gover.backend.process.services.ProcessInstanceAccessControlService;
import de.aivot.gover.backend.user.entities.UserEntity;
import de.aivot.gover.backend.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
                permissionService
        );
    }

    @Test
    void listPreservesProcessInstanceIdsOutsideIntegerRange() throws ResponseException {
        var pageable = PageRequest.of(0, 20);
        var filter = ProcessInstanceAccessControlFilter.create();
        var processInstanceId = (long) Integer.MAX_VALUE + 1;
        when(permissionService.getProcessInstancesWithPermission(
                "user-1",
                ProcessPermissionProvider.PROCESS_INSTANCE_UPDATE
        )).thenReturn(List.of(processInstanceId));
        when(accessControlService.list(pageable, filter)).thenReturn(Page.empty(pageable));

        controller.list(jwt, pageable, filter);

        assertEquals(List.of(processInstanceId), filter.getTargetProcessInstanceIds());
        verify(accessControlService).list(pageable, filter);
    }
}
