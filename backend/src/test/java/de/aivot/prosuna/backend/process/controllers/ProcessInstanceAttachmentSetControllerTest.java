package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import de.aivot.prosuna.backend.process.filters.ProcessInstanceAttachmentSetFilter;
import de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentSetService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessInstanceAttachmentSetControllerTest {
    private final Jwt jwt = mock(Jwt.class);
    private final UserService userService = mock(UserService.class);
    private final ProcessInstanceAttachmentSetService attachmentSetService = mock(ProcessInstanceAttachmentSetService.class);
    private final PermissionService permissionService = mock(PermissionService.class);
    private final UserEntity user = mock(UserEntity.class);

    private ProcessInstanceAttachmentSetController controller;

    @BeforeEach
    void setUp() throws ResponseException {
        when(user.getId()).thenReturn("user-1");
        when(userService.fromJWT(jwt)).thenReturn(Optional.of(user));
        controller = new ProcessInstanceAttachmentSetController(
                userService,
                attachmentSetService,
                permissionService
        );
    }

    @Test
    void listIntersectsRequestedAndAccessibleProcessInstances() throws ResponseException {
        var pageable = PageRequest.of(0, 20);
        var filter = ProcessInstanceAttachmentSetFilter.create()
                .setProcessInstanceIds(List.of(12L, 13L));
        when(permissionService.getProcessInstancesWithPermission(
                "user-1",
                ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ
        )).thenReturn(List.of(11L, 12L));
        when(attachmentSetService.list(pageable, filter)).thenReturn(Page.empty(pageable));

        controller.list(jwt, pageable, filter);

        assertEquals(List.of(12L), filter.getProcessInstanceIds());
        verify(attachmentSetService).list(pageable, filter);
    }

    @Test
    void retrieveRequiresReadPermissionForOwningProcessInstance() throws ResponseException {
        var attachmentSet = new ProcessInstanceAttachmentSetEntity()
                .setId(7)
                .setProcessInstanceId(12L);
        when(attachmentSetService.retrieve(7)).thenReturn(Optional.of(attachmentSet));

        assertEquals(attachmentSet, controller.retrieve(jwt, 7));
        verify(permissionService).requireProcessInstancePermission(
                "user-1",
                12L,
                ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ
        );
    }
}
