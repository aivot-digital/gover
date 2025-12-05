package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsAndPermissionsEntity;
import de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsEntity;
import de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsEntityId;
import de.aivot.GoverBackend.form.entities.VFormWithPermissionsEntity;
import de.aivot.GoverBackend.form.filters.VFormVersionWithDetailsAndPermissionFilter;
import de.aivot.GoverBackend.form.filters.VFormVersionWithDetailsFilter;
import de.aivot.GoverBackend.form.services.FormVersionService;
import de.aivot.GoverBackend.form.services.VFormVersionWithDetailsAndPermissionsService;
import de.aivot.GoverBackend.form.services.VFormVersionWithDetailsService;
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
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/form-versions-with-details/")
@Tag(
        name = "Form Versions",
        description = "Form versions represent different iterations of a form. " +
                      "They allow for version control of a form over time. " +
                      "Versions can be published and revoken but only one version can be published at a time."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class VFormVersionWithDetailsController {
    private final FormVersionService formVersionService;
    private final VFormVersionWithDetailsService vFormVersionWithDetailsService;
    private final VFormVersionWithDetailsAndPermissionsService vFormVersionWithDetailsAndPermissionsService;
    private final VFormWithPermissionsService vFormWithPermissionsService;
    private final UserService userService;

    @Autowired
    public VFormVersionWithDetailsController(FormVersionService formVersionService,
                                             VFormVersionWithDetailsService vFormVersionWithDetailsService,
                                             VFormVersionWithDetailsAndPermissionsService vFormVersionWithDetailsAndPermissionsService,
                                             VFormWithPermissionsService vFormWithPermissionsService,
                                             UserService userService) {
        this.formVersionService = formVersionService;
        this.vFormVersionWithDetailsService = vFormVersionWithDetailsService;
        this.vFormVersionWithDetailsAndPermissionsService = vFormVersionWithDetailsAndPermissionsService;
        this.vFormWithPermissionsService = vFormWithPermissionsService;
        this.userService = userService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Form Versions With Details",
            description = "List all form versions with their details. " +
                          "Supports pagination and filtering based on various criteria."
    )
    public Page<VFormVersionWithDetailsEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid VFormVersionWithDetailsFilter filter
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (user.getIsSuperAdmin()) {
            return vFormVersionWithDetailsService
                    .list(pageable, filter);
        }

        var vFilter = VFormVersionWithDetailsAndPermissionFilter
                .from(filter);

        vFilter.setUserId(user.getId());
        vFilter.setFormPermissionRead(true);

        return vFormVersionWithDetailsAndPermissionsService
                .list(pageable, vFilter)
                .map(VFormVersionWithDetailsAndPermissionsEntity::toVFormVersionWithDetailsEntity);
    }

    @GetMapping("{formId}/latest/")
    @Operation(
            summary = "Retrieve Latest Form Version With Details",
            description = "Retrieve the latest version of a form with its details. " +
                          "Requires \"" + PermissionLabels.FormPermissionRead + "\" permission."
    )
    public VFormVersionWithDetailsEntity retrieveLatest(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId
    ) throws ResponseException {
        var latestVersion = formVersionService
                .getLatestVersion(formId)
                .orElseThrow(ResponseException::notFound)
                .getVersion();

        return retrieveVersion(jwt, formId, latestVersion);
    }

    @GetMapping("{formId}/{version}/")
    @Operation(
            summary = "Retrieve Specific Form Version With Details",
            description = "Retrieve a specific version of a form with its details. " +
                          "Requires \"" + PermissionLabels.FormPermissionRead + "\" permission."
    )
    public VFormVersionWithDetailsEntity retrieveVersion(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId,
            @Nonnull @PathVariable Integer version
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        vFormWithPermissionsService
                .checkUserPermission(
                        formId,
                        user.getId(),
                        VFormWithPermissionsEntity::getFormPermissionRead,
                        PermissionLabels.FormPermissionRead);

        var id = new VFormVersionWithDetailsEntityId(formId, version);

        return vFormVersionWithDetailsService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }
}
