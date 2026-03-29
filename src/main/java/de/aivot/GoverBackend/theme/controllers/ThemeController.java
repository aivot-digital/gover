package de.aivot.GoverBackend.theme.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.theme.dtos.ThemeRequestDTO;
import de.aivot.GoverBackend.theme.dtos.ThemeResponseDTO;
import de.aivot.GoverBackend.theme.entities.ThemeEntity;
import de.aivot.GoverBackend.theme.filters.ThemeFilter;
import de.aivot.GoverBackend.theme.services.ThemeService;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.utils.StringUtils;
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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;

@RestController
@RequestMapping("/api/themes/")
@Tag(name = "Themes", description = "Manage themes")
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ThemeController {
    private final ScopedAuditService auditService;
    private final ThemeService service;
    private final UserService userService;

    @Autowired
    public ThemeController(AuditService auditService,
                           ThemeService service, UserService userService) {
        this.auditService = auditService.createScopedAuditService(ThemeController.class, "Themes");
        this.service = service;
        this.userService = userService;
    }

    @GetMapping("")
    @Operation(
            summary = "List themes",
            description = "Retrieve a paginated list of themes. Supports filtering."
    )
    public Page<ThemeResponseDTO> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid ThemeFilter filter
    ) throws ResponseException {
        return service
                .list(pageable, filter)
                .map(ThemeResponseDTO::fromEntity);
    }

    @PostMapping("")
    @Operation(
            summary = "Create theme",
            description = "Create a new theme. Requires system admin permissions."
    )
    public ThemeResponseDTO create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody ThemeRequestDTO newThemeRequest
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSystemAdmin()
                .orElseThrow(ResponseException::noSystemAdminPermission);

        var newTheme = newThemeRequest
                .toEntity();

        var createdTheme = service
                .create(newTheme);

        auditService
                .create()
                .withUser(user)
                .withAuditAction(
                        AuditAction.Create,
                        ThemeEntity.class,
                        createdTheme.getId(),
                        "id",
                        Map.of(
                                "id", createdTheme.getId(),
                                "name", createdTheme.getName()
                        ))
                .withMessage(
                        "Das Theme %s mit der ID %s wurde von der Mitarbeiter:in %s erstellt.",
                        StringUtils.quote(createdTheme.getName()),
                        StringUtils.quote(String.valueOf(createdTheme.getId())),
                        StringUtils.quote(user.getFullName())
                )
                .log();

        return ThemeResponseDTO
                .fromEntity(createdTheme);
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve theme",
            description = "Retrieve a specific theme by its ID."
    )
    public ThemeResponseDTO retrieve(
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        return service
                .retrieve(id)
                .map(ThemeResponseDTO::fromEntity)
                .orElseThrow(ResponseException::notFound);
    }


    @PutMapping("{id}/")
    @Operation(
            summary = "Update theme",
            description = "Update an existing theme. Requires system admin permissions."
    )
    public ThemeResponseDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @Valid @RequestBody ThemeRequestDTO changeThemeRequest
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSystemAdmin()
                .orElseThrow(ResponseException::noSystemAdminPermission);

        var changedTheme = changeThemeRequest
                .toEntity();

        var updatedTheme = service
                .update(id, changedTheme);

        auditService
                .create()
                .withUser(user)
                .withAuditAction(
                        AuditAction.Update,
                        ThemeEntity.class,
                        updatedTheme.getId(),
                        "id",
                        Map.of(
                                "id", updatedTheme.getId(),
                                "name", updatedTheme.getName()
                        ))
                .withMessage(
                        "Das Theme %s mit der ID %s wurde von der Mitarbeiter:in %s aktualisiert.",
                        StringUtils.quote(updatedTheme.getName()),
                        StringUtils.quote(String.valueOf(updatedTheme.getId())),
                        StringUtils.quote(user.getFullName())
                )
                .log();

        return ThemeResponseDTO
                .fromEntity(updatedTheme);
    }

    @DeleteMapping("{id}/")
    @Operation(
            summary = "Delete theme",
            description = "Delete an existing theme. Requires system admin permissions."
    )
    public void destroy(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSystemAdmin()
                .orElseThrow(ResponseException::noSystemAdminPermission);

        var deletedTheme = service
                .delete(id);

        auditService
                .create()
                .withUser(user)
                .withAuditAction(
                        AuditAction.Delete,
                        ThemeEntity.class,
                        deletedTheme.getId(),
                        "id",
                        Map.of(
                                "id", deletedTheme.getId(),
                                "name", deletedTheme.getName()
                        ))
                .withMessage(
                        "Das Theme %s mit der ID %s wurde von der Mitarbeiter:in %s gelöscht.",
                        StringUtils.quote(deletedTheme.getName()),
                        StringUtils.quote(String.valueOf(deletedTheme.getId())),
                        StringUtils.quote(user.getFullName())
                )
                .log();
    }
}
