package de.aivot.prosuna.backend.theme.controllers;

import de.aivot.prosuna.backend.audit.enums.AuditAction;
import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.audit.services.ScopedAuditService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.theme.dtos.ThemeRequestDTO;
import de.aivot.prosuna.backend.theme.dtos.ThemeResponseDTO;
import de.aivot.prosuna.backend.theme.entities.ThemeEntity;
import de.aivot.prosuna.backend.theme.filters.ThemeFilter;
import de.aivot.prosuna.backend.theme.permissions.ThemePermissionProvider;
import de.aivot.prosuna.backend.theme.services.ThemeService;
import de.aivot.prosuna.backend.user.services.UserService;
import de.aivot.prosuna.backend.utils.StringUtils;
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
@Tag(name = "Themes", description = "Manage appearance settings")
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ThemeController {
    private final ScopedAuditService auditService;
    private final ThemeService service;
    private final UserService userService;
    private final PermissionService permissionService;

    @Autowired
    public ThemeController(AuditService auditService,
                           ThemeService service,
                           UserService userService,
                           PermissionService permissionService) {
        this.auditService = auditService.createScopedAuditService(ThemeController.class, "Erscheinungsbilder");
        this.service = service;
        this.userService = userService;
        this.permissionService = permissionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List themes",
            description = "Retrieve a paginated list of themes. Supports filtering. " +
                    "Requires the system-level permission `" + ThemePermissionProvider.THEME_READ + "`."
    )
    public Page<ThemeResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid ThemeFilter filter
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .requireSystemPermission(user.getId(), ThemePermissionProvider.THEME_READ);

        return service
                .list(pageable, filter)
                .map(ThemeResponseDTO::fromEntity);
    }

    @PostMapping("")
    @Operation(
            summary = "Create theme",
            description = "Create a new theme. " +
                    "Requires the system-level permission `" + ThemePermissionProvider.THEME_CREATE + "`."
    )
    public ThemeResponseDTO create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody ThemeRequestDTO newThemeRequest
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .requireSystemPermission(user.getId(), ThemePermissionProvider.THEME_CREATE);

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
                        "Das Erscheinungsbild %s mit der ID %s wurde von der Mitarbeiter:in %s erstellt.",
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
            description = "Retrieve a specific theme by its ID. " +
                    "Requires the system-level permission `" + ThemePermissionProvider.THEME_READ + "`."
    )
    public ThemeResponseDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .requireSystemPermission(user.getId(), ThemePermissionProvider.THEME_READ);

        return service
                .retrieve(id)
                .map(ThemeResponseDTO::fromEntity)
                .orElseThrow(ResponseException::notFound);
    }


    @PutMapping("{id}/")
    @Operation(
            summary = "Update theme",
            description = "Update an existing theme. " +
                    "Requires the system-level permission `" + ThemePermissionProvider.THEME_UPDATE + "`."
    )
    public ThemeResponseDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @Valid @RequestBody ThemeRequestDTO changeThemeRequest
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .requireSystemPermission(user.getId(), ThemePermissionProvider.THEME_UPDATE);

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
                        "Das Erscheinungsbild %s mit der ID %s wurde von der Mitarbeiter:in %s aktualisiert.",
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
            description = "Delete an existing theme. " +
                    "Requires the system-level permission `" + ThemePermissionProvider.THEME_DELETE + "`."
    )
    public void destroy(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .requireSystemPermission(user.getId(), ThemePermissionProvider.THEME_DELETE);

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
                        "Das Erscheinungsbild %s mit der ID %s wurde von der Mitarbeiter:in %s gelöscht.",
                        StringUtils.quote(deletedTheme.getName()),
                        StringUtils.quote(String.valueOf(deletedTheme.getId())),
                        StringUtils.quote(user.getFullName())
                )
                .log();
    }
}
