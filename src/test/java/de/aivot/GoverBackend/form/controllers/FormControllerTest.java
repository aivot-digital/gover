package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.audit.services.AuditLogService;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.department.services.VDepartmentMembershipWithPermissionsService;
import de.aivot.GoverBackend.form.entities.FormEntity;
import de.aivot.GoverBackend.form.entities.VFormWithPermissionsEntity;
import de.aivot.GoverBackend.form.services.FormLockService;
import de.aivot.GoverBackend.form.services.FormRevisionService;
import de.aivot.GoverBackend.form.services.FormService;
import de.aivot.GoverBackend.form.services.FormVersionService;
import de.aivot.GoverBackend.form.services.VFormVersionWithDetailsService;
import de.aivot.GoverBackend.form.services.VFormWithPermissionsService;
import de.aivot.GoverBackend.mail.services.ExceptionMailService;
import de.aivot.GoverBackend.mail.services.FormMailService;
import de.aivot.GoverBackend.system.properties.BuildProperties;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.userRoles.data.PermissionLabels;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FormControllerTest {
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private FormMailService formMailService;
    @Mock
    private ExceptionMailService exceptionMailService;
    @Mock
    private FormService formService;
    @Mock
    private FormLockService formLockService;
    @Mock
    private FormRevisionService formRevisionService;
    @Mock
    private VFormWithPermissionsService vFormWithPermissionsService;
    @Mock
    private VDepartmentMembershipWithPermissionsService vDepartmentMembershipWithPermissionsService;
    @Mock
    private VFormVersionWithDetailsService vFormVersionWithDetailsService;
    @Mock
    private FormVersionService formVersionService;
    @Mock
    private BuildProperties buildProperties;
    @Mock
    private UserService userService;

    private FormController controller;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        controller = new FormController(
                new AuditService(auditLogService),
                formMailService,
                exceptionMailService,
                formService,
                formLockService,
                formRevisionService,
                vFormWithPermissionsService,
                vDepartmentMembershipWithPermissionsService,
                vFormVersionWithDetailsService,
                formVersionService,
                buildProperties,
                userService
        );

        jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of("sub", "user-1")
        );
    }

    @Test
    void destroyShouldRequireDeletePermission() throws Exception {
        var user = mock(UserEntity.class);
        when(userService.fromJWT(jwt)).thenReturn(Optional.of(user));
        when(user.getIsSuperAdmin()).thenReturn(false);
        when(user.getId()).thenReturn("user-1");
        when(user.getFullName()).thenReturn("Jane Doe");

        var form = new FormEntity()
                .setId(42)
                .setSlug("service-request")
                .setInternalTitle("Antrag")
                .setDevelopingDepartmentId(7)
                .setVersionCount(1);

        when(formService.retrieve(42)).thenReturn(Optional.of(form));
        when(formService.delete(42)).thenReturn(form);

        controller.destroy(jwt, 42);

        verify(vFormWithPermissionsService).checkUserPermission(
                eq(42),
                eq("user-1"),
                any(),
                eq(PermissionLabels.FormPermissionDelete)
        );
    }
}
