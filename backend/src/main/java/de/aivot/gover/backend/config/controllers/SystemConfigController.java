package de.aivot.gover.backend.config.controllers;

import de.aivot.gover.backend.audit.enums.AuditAction;
import de.aivot.gover.backend.audit.services.AuditService;
import de.aivot.gover.backend.audit.services.ScopedAuditService;
import de.aivot.gover.backend.config.dtos.SystemConfigRequestDto;
import de.aivot.gover.backend.config.dtos.SystemConfigResponseDto;
import de.aivot.gover.backend.config.entities.SystemConfigEntity;
import de.aivot.gover.backend.config.filters.SystemConfigFilter;
import de.aivot.gover.backend.config.models.SystemConfigDefinition;
import de.aivot.gover.backend.config.permissions.ConfigPermissionProvider;
import de.aivot.gover.backend.config.services.SystemConfigService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.openApi.OpenApiConstants;
import de.aivot.gover.backend.permissions.services.PermissionService;
import de.aivot.gover.backend.user.services.UserService;
import de.aivot.gover.backend.utils.StringUtils;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * This controller provides functionality to list, retrieve and update system configurations.
 */
@RestController
@RequestMapping("/api/system-configs/")
@Tag(
        name = OpenApiConstants.Tags.SystemConfigsName,
        description = OpenApiConstants.Tags.SystemConfigDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class SystemConfigController {
    private final ScopedAuditService auditService;
    private final SystemConfigService systemConfigService;
    private final UserService userService;
    private final PermissionService permissionService;

    @Autowired
    public SystemConfigController(AuditService auditService,
                                  SystemConfigService systemConfigService,
                                  UserService userService,
                                  PermissionService permissionService) {
        this.auditService = auditService.createScopedAuditService(SystemConfigController.class, "Systemkonfiguration");
        this.systemConfigService = systemConfigService;
        this.userService = userService;
        this.permissionService = permissionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List System Configurations",
            description = "Retrieve a paginated list of system configurations with optional filtering. " +
                    "Requires the system-level permission `" + ConfigPermissionProvider.SYSTEM_CONFIG_READ + "`."
    )
    public Page<SystemConfigResponseDto> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid SystemConfigFilter filter
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, ConfigPermissionProvider.SYSTEM_CONFIG_READ);

        return systemConfigService
                .list(pageable, filter)
                .map(entity -> {
                    var def = systemConfigService
                            .getDefinition(entity.getKey())
                            .orElseThrow(RuntimeException::new); // This should never happen

                    try {
                        return SystemConfigResponseDto.fromEntity(entity, def);
                    } catch (ResponseException e) {
                        throw new RuntimeException(e); // This should never happen
                    }
                });
    }

    @GetMapping("definitions/")
    @Operation(
            summary = "List System Configuration Definitions",
            description = "Retrieve a list of all system configuration definitions. This endpoint can be used to get metadata about the available system configurations, such as their types, categories and descriptions. This is especially useful for clients to dynamically adapt to available configurations. " +
                    "Requires the system-level permission `" + ConfigPermissionProvider.SYSTEM_CONFIG_READ + "`."
    )
    public List<SystemConfigDefinition<?>> list(
            @Nullable @AuthenticationPrincipal Jwt jwt
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, ConfigPermissionProvider.SYSTEM_CONFIG_READ);

        return systemConfigService
                .getSystemConfigDefinitions();
    }

    @PutMapping("{key}/")
    @Operation(
            summary = "Update System Configuration",
            description = "Update the value of a specific system configuration identified by its key. " +
                    "Requires the system-level permission `" + ConfigPermissionProvider.SYSTEM_CONFIG_UPDATE + "`."
    )
    public SystemConfigResponseDto update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String key,
            @Nonnull @RequestBody @Valid SystemConfigRequestDto updateRequest
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .requireSystemPermission(user.getId(), ConfigPermissionProvider.SYSTEM_CONFIG_UPDATE);

        var def = systemConfigService
                .getDefinition(key)
                .orElseThrow(() -> ResponseException.notFound("Für den Schlüssel \"" + key + "\" wurde keine Systemkonfiguration gefunden."));

        var entity = updateRequest
                .toEntity();

        var config = systemConfigService
                .save(key, entity, Boolean.TRUE.equals(updateRequest.changeConfirmed()));

        // Log the action of updating the system configuration
        auditService
                .create()
                .withUser(user)
                .withAuditAction(
                        AuditAction.Update,
                        SystemConfigEntity.class,
                        config.getKey(),
                        "key"
                )
                .withMessage(
                        "Die Systemkonfiguration %s wurde von der Mitarbeiter:in %s aktualisiert.",
                        StringUtils.quote(config.getKey()),
                        StringUtils.quote(user.getFullName())
                )
                .log();

        return SystemConfigResponseDto
                .fromEntity(config, def);
    }
}
