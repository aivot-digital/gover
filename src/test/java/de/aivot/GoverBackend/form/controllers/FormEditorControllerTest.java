package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.form.entities.VFormWithPermissionsEntity;
import de.aivot.GoverBackend.form.entities.VFormWithPermissionsEntityId;
import de.aivot.GoverBackend.form.entities.projections.FormEditorProjection;
import de.aivot.GoverBackend.form.repositories.FormRepository;
import de.aivot.GoverBackend.form.services.VFormWithPermissionsService;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.userRoles.data.PermissionLabels;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FormEditorControllerTest {
    @Mock
    private FormRepository formRepository;
    @Mock
    private VFormWithPermissionsService vFormWithPermissionsService;
    @Mock
    private UserService userService;

    private FormEditorController controller;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new FormEditorController(formRepository, vFormWithPermissionsService, userService);
        jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of("sub", "user-1")
        );
    }

    @Test
    void listFormEditorsForFormsShouldFilterUnreadableForms() throws Exception {
        var user = mock(UserEntity.class);
        when(userService.fromJWT(jwt)).thenReturn(Optional.of(user));
        when(user.getIsSuperAdmin()).thenReturn(false);
        when(user.getId()).thenReturn("user-1");

        when(vFormWithPermissionsService.retrieve(new VFormWithPermissionsEntityId(1, "user-1")))
                .thenReturn(Optional.of(new VFormWithPermissionsEntity().setFormPermissionRead(true)));
        when(vFormWithPermissionsService.retrieve(new VFormWithPermissionsEntityId(2, "user-1")))
                .thenReturn(Optional.empty());

        var projection = mock(FormEditorProjection.class);
        when(formRepository.findAllByFormIdIn(List.of(1))).thenReturn(List.of(projection));

        var result = controller.listFormEditorsForForms(jwt, List.of(1, 2));

        assertEquals(1, result.size());
        verify(formRepository).findAllByFormIdIn(List.of(1));
    }

    @Test
    void listFormEditorsForVersionsShouldRequireReadPermission() throws Exception {
        var user = mock(UserEntity.class);
        when(userService.fromJWT(jwt)).thenReturn(Optional.of(user));
        when(user.getIsSuperAdmin()).thenReturn(false);
        when(user.getId()).thenReturn("user-1");
        when(formRepository.findAllByFormId(9)).thenReturn(List.of());

        controller.listFormEditorsForVersions(jwt, 9);

        verify(vFormWithPermissionsService).checkUserPermission(
                eq(9),
                eq("user-1"),
                any(),
                eq(PermissionLabels.FormPermissionRead)
        );
    }
}
