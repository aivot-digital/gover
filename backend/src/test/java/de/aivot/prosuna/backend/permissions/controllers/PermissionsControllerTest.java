package de.aivot.prosuna.backend.permissions.controllers;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.entities.VUserDepartmentPermissionEntity;
import de.aivot.prosuna.backend.permissions.entities.VUserSystemPermissionEntity;
import de.aivot.prosuna.backend.permissions.entities.VUserTeamPermissionEntity;
import de.aivot.prosuna.backend.permissions.projections.DomainPermissionProjection;
import de.aivot.prosuna.backend.permissions.projections.ProcessInstancePermissionProjection;
import de.aivot.prosuna.backend.permissions.projections.ProcessPermissionProjection;
import de.aivot.prosuna.backend.permissions.repositories.VUserDepartmentPermissionRepository;
import de.aivot.prosuna.backend.permissions.repositories.VUserDomainPermissionRepository;
import de.aivot.prosuna.backend.permissions.repositories.VUserSystemPermissionRepository;
import de.aivot.prosuna.backend.permissions.repositories.VUserTeamPermissionRepository;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.repositories.VUserProcessAccessPermissionsRepository;
import de.aivot.prosuna.backend.process.repositories.VUserProcessInstanceAccessPermissionsRepository;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PermissionsControllerTest {
    @Test
    void listForSelfBuildsPermissionSetFromRepositoryProjections() throws ResponseException {
        var departmentPermissionRepository = mock(VUserDepartmentPermissionRepository.class);
        var teamPermissionRepository = mock(VUserTeamPermissionRepository.class);
        var systemPermissionRepository = mock(VUserSystemPermissionRepository.class);
        var domainPermissionRepository = mock(VUserDomainPermissionRepository.class);
        var processPermissionRepository = mock(VUserProcessAccessPermissionsRepository.class);
        var processInstancePermissionRepository = mock(VUserProcessInstanceAccessPermissionsRepository.class);
        var permissionService = mock(PermissionService.class);
        var userService = mock(UserService.class);
        var jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of("sub", "user-1")
        );
        var user = new UserEntity().setId("user-1");
        var departmentPermission = mock(VUserDepartmentPermissionEntity.class);
        var teamPermission = mock(VUserTeamPermissionEntity.class);
        var systemPermission = mock(VUserSystemPermissionEntity.class);
        var domainPermission = domainPermission(10, null, List.of("department.read"));
        var processPermission = processPermission(null, 10, 20, List.of("process_definition.read"));
        var processInstancePermission = processInstancePermission(30, null, 40L, List.of("process_instance.read"));

        when(userService.fromJWT(jwt)).thenReturn(Optional.of(user));
        when(departmentPermissionRepository.findAllByUserId("user-1")).thenReturn(List.of(departmentPermission));
        when(teamPermissionRepository.findAllByUserId("user-1")).thenReturn(List.of(teamPermission));
        when(systemPermissionRepository.findAllByUserId("user-1")).thenReturn(List.of(systemPermission));
        when(domainPermissionRepository.findAllConcreteByUserId("user-1")).thenReturn(List.of(domainPermission));
        when(processPermissionRepository.findAllConcreteByUserId("user-1")).thenReturn(List.of(processPermission));
        when(processInstancePermissionRepository.findAllConcreteByUserId("user-1"))
                .thenReturn(List.of(processInstancePermission));

        var controller = new PermissionsController(
                List.of(),
                departmentPermissionRepository,
                teamPermissionRepository,
                systemPermissionRepository,
                domainPermissionRepository,
                processPermissionRepository,
                processInstancePermissionRepository,
                permissionService,
                userService
        );

        var result = controller.listForSelf(jwt);

        assertEquals(List.of(departmentPermission), result.departmentPermissions());
        assertEquals(List.of(teamPermission), result.teamPermissions());
        assertEquals(List.of(systemPermission), result.systemPermissions());
        assertEquals(
                List.of(new PermissionsController.DomainPermission(
                        "department:10",
                        "user-1",
                        10,
                        null,
                        List.of("department.read")
                )),
                result.domainPermissions()
        );
        assertEquals(
                List.of(new PermissionsController.ProcessPermission(
                        "process:20:team::department:10",
                        "user-1",
                        null,
                        10,
                        20,
                        List.of("process_definition.read")
                )),
                result.processPermissions()
        );
        assertEquals(
                List.of(new PermissionsController.ProcessInstancePermission(
                        "process-instance:40:team:30:department:",
                        "user-1",
                        30,
                        null,
                        40L,
                        List.of("process_instance.read")
                )),
                result.processInstancePermissions()
        );
        verify(domainPermissionRepository).findAllConcreteByUserId("user-1");
        verify(processPermissionRepository).findAllConcreteByUserId("user-1");
        verify(processInstancePermissionRepository).findAllConcreteByUserId("user-1");
    }

    private DomainPermissionProjection domainPermission(Integer departmentId,
                                                        Integer teamId,
                                                        List<String> permissions) {
        var projection = mock(DomainPermissionProjection.class);
        when(projection.getUserId()).thenReturn("user-1");
        when(projection.getDepartmentId()).thenReturn(departmentId);
        when(projection.getTeamId()).thenReturn(teamId);
        when(projection.getPermissions()).thenReturn(permissions);
        return projection;
    }

    private ProcessPermissionProjection processPermission(Integer teamId,
                                                          Integer departmentId,
                                                          Integer processId,
                                                          List<String> permissions) {
        var projection = mock(ProcessPermissionProjection.class);
        when(projection.getUserId()).thenReturn("user-1");
        when(projection.getViaSourceTeamId()).thenReturn(teamId);
        when(projection.getViaSourceDepartmentId()).thenReturn(departmentId);
        when(projection.getProcessId()).thenReturn(processId);
        when(projection.getPermissions()).thenReturn(permissions);
        return projection;
    }

    private ProcessInstancePermissionProjection processInstancePermission(Integer teamId,
                                                                          Integer departmentId,
                                                                          Long processInstanceId,
                                                                          List<String> permissions) {
        var projection = mock(ProcessInstancePermissionProjection.class);
        when(projection.getUserId()).thenReturn("user-1");
        when(projection.getViaSourceTeamId()).thenReturn(teamId);
        when(projection.getViaSourceDepartmentId()).thenReturn(departmentId);
        when(projection.getProcessInstanceId()).thenReturn(processInstanceId);
        when(projection.getPermissions()).thenReturn(permissions);
        return projection;
    }
}
