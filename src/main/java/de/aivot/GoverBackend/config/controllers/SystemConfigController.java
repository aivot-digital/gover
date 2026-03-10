package de.aivot.GoverBackend.config.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.config.dtos.SystemConfigRequestDto;
import de.aivot.GoverBackend.config.dtos.SystemConfigResponseDto;
import de.aivot.GoverBackend.config.entities.SystemConfigEntity;
import de.aivot.GoverBackend.config.filters.SystemConfigFilter;
import de.aivot.GoverBackend.config.services.SystemConfigService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.utils.StringUtils;
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

    @Autowired
    public SystemConfigController(AuditService auditService,
                                  SystemConfigService systemConfigService,
                                  UserService userService) {
        this.auditService = auditService.createScopedAuditService(SystemConfigController.class, "Systemkonfiguration");
        this.systemConfigService = systemConfigService;
        this.userService = userService;
    }

    @GetMapping("")
    @Operation(
            summary = "List System Configurations",
            description = "Retrieve a paginated list of system configurations with optional filtering."
    )
    public Page<SystemConfigResponseDto> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid SystemConfigFilter filter
    ) throws ResponseException {
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

    @PutMapping("{key}/")
    @Operation(
            summary = "Update System Configuration",
            description = "Update the value of a specific system configuration identified by its key. " +
                    "Requires system administrator permissions."
    )
    public SystemConfigResponseDto update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String key,
            @Nonnull @RequestBody @Valid SystemConfigRequestDto updateRequest
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSystemAdmin()
                .orElseThrow(ResponseException::noSystemAdminPermission);

        var def = systemConfigService
                .getDefinition(key)
                .orElseThrow(() -> ResponseException.notFound("Für den Schlüssel \"" + key + "\" wurde keine Systemkonfiguration gefunden."));

        var entity = updateRequest
                .toEntity(def);

        var config = systemConfigService
                .save(key, entity);

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
