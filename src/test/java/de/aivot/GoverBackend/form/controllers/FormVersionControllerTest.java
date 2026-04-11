package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.audit.services.AuditLogService;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.GoverBackend.form.entities.FormVersionEntity;
import de.aivot.GoverBackend.form.entities.FormVersionEntityId;
import de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsEntity;
import de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsEntityId;
import de.aivot.GoverBackend.form.services.FormLockService;
import de.aivot.GoverBackend.form.services.FormRevisionService;
import de.aivot.GoverBackend.form.services.FormService;
import de.aivot.GoverBackend.form.services.FormVersionService;
import de.aivot.GoverBackend.form.services.VFormVersionWithDetailsAndPermissionsService;
import de.aivot.GoverBackend.form.services.VFormVersionWithDetailsService;
import de.aivot.GoverBackend.form.services.VFormWithPermissionsService;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FormVersionControllerTest {
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private FormVersionService formVersionService;
    @Mock
    private FormService formService;
    @Mock
    private FormLockService formLockService;
    @Mock
    private VFormWithPermissionsService vFormWithPermissionsService;
    @Mock
    private VFormVersionWithDetailsAndPermissionsService vFormVersionWithDetailsAndPermissionsService;
    @Mock
    private FormRevisionService formRevisionService;
    @Mock
    private VFormVersionWithDetailsService vFormVersionWithDetailsService;
    @Mock
    private UserService userService;

    private FormVersionController controller;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        controller = new FormVersionController(
                new AuditService(auditLogService),
                formVersionService,
                formService,
                formLockService,
                vFormWithPermissionsService,
                vFormVersionWithDetailsAndPermissionsService,
                formRevisionService,
                vFormVersionWithDetailsService,
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
    void updateShouldUseFreshViewSnapshotForRevisionDiff() throws Exception {
        var user = mock(UserEntity.class);
        when(userService.fromJWT(jwt)).thenReturn(Optional.of(user));
        when(user.getIsSuperAdmin()).thenReturn(true);
        when(user.getId()).thenReturn("user-1");
        when(user.getFullName()).thenReturn("Jane Doe");

        var formVersionId = new VFormVersionWithDetailsEntityId(42, 3);
        var oldView = new VFormVersionWithDetailsEntity()
                .setId(42)
                .setFormId(42)
                .setVersion(3)
                .setPublicTitle("Old title");
        var newView = new VFormVersionWithDetailsEntity()
                .setId(42)
                .setFormId(42)
                .setVersion(3)
                .setPublicTitle("New title");

        when(vFormVersionWithDetailsService.retrieve(formVersionId)).thenReturn(Optional.of(oldView));
        when(vFormVersionWithDetailsService.retrieveFresh(formVersionId)).thenReturn(Optional.of(newView));

        var patchedFormVersion = new FormVersionEntity()
                .setFormId(42)
                .setVersion(3)
                .setPublicTitle("New title")
                .setRootElement(new FormLayoutElement());
        var updatedFormVersion = new FormVersionEntity()
                .setFormId(42)
                .setVersion(3)
                .setPublicTitle("New title")
                .setRootElement(new FormLayoutElement());

        when(formVersionService.update(FormVersionEntityId.of(42, 3), patchedFormVersion)).thenReturn(updatedFormVersion);

        controller.update(jwt, 42, 3, patchedFormVersion);

        verify(vFormVersionWithDetailsService).retrieveFresh(formVersionId);
        verify(formRevisionService).create(
                any(),
                argThat((Map<String, Object> value) -> "New title".equals(value.get("publicTitle"))),
                argThat((Map<String, Object> value) -> "Old title".equals(value.get("publicTitle")))
        );
    }
}
