package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.form.entities.VFormWithPermissionsEntity;
import de.aivot.GoverBackend.form.entities.VFormWithPermissionsEntityId;
import de.aivot.GoverBackend.form.entities.projections.FormEditorProjection;
import de.aivot.GoverBackend.form.repositories.FormRepository;
import de.aivot.GoverBackend.form.services.VFormWithPermissionsService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.userRoles.data.PermissionLabels;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/form-editors/")
@Tag(
        name = "Forms",
        description = "Forms are built for collecting data from users. " +
                      "They can be designed with various elements and configurations to suit different data collection needs. " +
                      "Forms can be published, managed, and analyzed within the system."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class FormEditorController {
    private final FormRepository formRepository;
    private final VFormWithPermissionsService vFormWithPermissionsService;
    private final UserService userService;

    @Autowired
    public FormEditorController(FormRepository formRepository,
                                VFormWithPermissionsService vFormWithPermissionsService,
                                UserService userService) {
        this.formRepository = formRepository;
        this.vFormWithPermissionsService = vFormWithPermissionsService;
        this.userService = userService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Form Editors for Forms",
            description = "Retrieve a list of form editors associated with the specified form IDs."
    )
    public List<FormEditorProjection> listFormEditorsForForms(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestParam List<Integer> formIds
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (formIds.isEmpty()) {
            return List.of();
        }

        if (!execUser.getIsSuperAdmin()) {
            formIds = formIds
                    .stream()
                    .filter(formId -> hasReadPermission(formId, execUser.getId()))
                    .toList();
        }

        if (formIds.isEmpty()) {
            return List.of();
        }

        return formRepository
                .findAllByFormIdIn(formIds);
    }

    @GetMapping("{formId}/")
    @Operation(
            summary = "List Form Editors for a Form's Versions",
            description = "Retrieve a list of form editors associated with all versions of the specified form ID."
    )
    public List<FormEditorProjection> listFormEditorsForVersions(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!execUser.getIsSuperAdmin()) {
            vFormWithPermissionsService.checkUserPermission(
                    formId,
                    execUser.getId(),
                    VFormWithPermissionsEntity::getFormPermissionRead,
                    PermissionLabels.FormPermissionRead
            );
        }

        return formRepository
                .findAllByFormId(formId);
    }

    private boolean hasReadPermission(@Nonnull Integer formId, @Nonnull String userId) {
        return vFormWithPermissionsService
                .retrieve(new VFormWithPermissionsEntityId(formId, userId))
                .map(VFormWithPermissionsEntity::getFormPermissionRead)
                .orElse(false);
    }
}
