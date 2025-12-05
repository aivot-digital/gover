package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.form.entities.VFormWithPermissionsEntity;
import de.aivot.GoverBackend.form.entities.VFormWithPermissionsEntityId;
import de.aivot.GoverBackend.form.filters.VFormWithPermissionsFilter;
import de.aivot.GoverBackend.form.services.FormService;
import de.aivot.GoverBackend.form.services.VFormWithPermissionsService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.user.services.UserService;
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
@RequestMapping("/api/form-with-permissions/")
@Tag(
        name = "Forms",
        description = "Forms are built for collecting data from users. " +
                      "They can be designed with various elements and configurations to suit different data collection needs. " +
                      "Forms can be published, managed, and analyzed within the system. " +
                      "Forms are versioned with the „Form Version” resource."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class VFormWithPermissionsController {
    private final VFormWithPermissionsService vFormWithPermissionsService;
    private final FormService formService;
    private final UserService userService;

    @Autowired
    public VFormWithPermissionsController(VFormWithPermissionsService vFormWithPermissionsService,
                                          FormService formService,
                                          UserService userService) {
        this.vFormWithPermissionsService = vFormWithPermissionsService;
        this.formService = formService;
        this.userService = userService;
    }

    @GetMapping("")
    @Operation(
            summary = "List forms with permissions",
            description = "List forms along with the permissions. " +
                          "Super admins can see all forms. " +
                          "Other users can only see forms they have permissions for."
    )
    public Page<VFormWithPermissionsEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid VFormWithPermissionsFilter filter
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (user.getIsSuperAdmin()) {
            return vFormWithPermissionsService
                    .list(pageable, filter);
        }

        filter.setUserId(user.getId());

        return vFormWithPermissionsService
                .list(pageable, filter);
    }

    @GetMapping("{formId}/{userId}/")
    @Operation(
            summary = "Retrieve form with permissions",
            description = "Retrieve a form along with the permissions for a specific user. " +
                          "Super admins can retrieve any form. " +
                          "Other users can only retrieve their own forms."
    )
    public VFormWithPermissionsEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId,
            @Nonnull @PathVariable String userId
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!user.getId().equals(userId) && !user.getIsSuperAdmin()) {
            throw ResponseException.forbidden();
        }

        if (user.getId().equals(userId) && user.getIsSuperAdmin()) {
            var form = formService
                    .retrieve(formId)
                    .orElseThrow(ResponseException::notFound);

            return new VFormWithPermissionsEntity(
                    form.getId(),
                    form.getSlug(),
                    form.getInternalTitle(),
                    form.getDevelopingDepartmentId(),
                    form.getCreated(),
                    form.getUpdated(),
                    form.getPublishedVersion(),
                    form.getDraftedVersion(),
                    form.getVersionCount(),
                    userId,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true
            );
        }

        var id = new VFormWithPermissionsEntityId(formId, userId);

        return vFormWithPermissionsService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }
}
