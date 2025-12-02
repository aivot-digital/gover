package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.form.entities.*;
import de.aivot.GoverBackend.form.filters.VFormVersionWithDetailsAndPermissionFilter;
import de.aivot.GoverBackend.form.filters.VFormVersionWithDetailsFilter;
import de.aivot.GoverBackend.form.services.*;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.security.OpenAPISecurityConfiguration;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.userRoles.data.PermissionLabels;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@RestController
@RequestMapping("/api/form-versions-with-details/")
@Tag(name = "FormVersion", description = "Interact with form versions")
public class VFormVersionWithDetailsController {
    private final FormVersionService formVersionService;
    private final VFormVersionWithDetailsService vFormVersionWithDetailsService;
    private final VFormVersionWithDetailsAndPermissionsService vFormVersionWithDetailsAndPermissionsService;
    private final VFormWithPermissionsService vFormWithPermissionsService;

    @Autowired
    public VFormVersionWithDetailsController(FormVersionService formVersionService,
                                             VFormVersionWithDetailsService vFormVersionWithDetailsService, VFormVersionWithDetailsAndPermissionsService vFormVersionWithDetailsAndPermissionsService, VFormWithPermissionsService vFormWithPermissionsService) {
        this.formVersionService = formVersionService;
        this.vFormVersionWithDetailsService = vFormVersionWithDetailsService;
        this.vFormVersionWithDetailsAndPermissionsService = vFormVersionWithDetailsAndPermissionsService;
        this.vFormWithPermissionsService = vFormWithPermissionsService;
    }

    @GetMapping("")
    @Operation(summary = "List Form Versions With Details", description = "List all form versions with their details.")
    @SecurityRequirement(name = OpenAPISecurityConfiguration.SecurityName)
    public Page<VFormVersionWithDetailsEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid VFormVersionWithDetailsFilter filter
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (user.getSuperAdmin()) {
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
    @Operation(summary = "Retrieve Latest Form Version With Details", description = "Retrieve the latest version of a form with its details.")
    @SecurityRequirement(name = OpenAPISecurityConfiguration.SecurityName)
    public VFormVersionWithDetailsEntity retrieveLatest(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var latestVersion = formVersionService
                .getLatestVersion(formId)
                .orElseThrow(ResponseException::notFound)
                .getVersion();

        return retrieveVersion(jwt, formId, latestVersion);
    }

    @GetMapping("{formId}/{version}/")
    @Operation(summary = "Retrieve Specific Form Version With Details", description = "Retrieve a specific version of a form with its details.")
    @SecurityRequirement(name = OpenAPISecurityConfiguration.SecurityName)
    public VFormVersionWithDetailsEntity retrieveVersion(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId,
            @Nonnull @PathVariable Integer version
    ) throws ResponseException {
        var user = UserService
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
