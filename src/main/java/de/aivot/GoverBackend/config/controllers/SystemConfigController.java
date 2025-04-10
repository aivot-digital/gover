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
import de.aivot.GoverBackend.user.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * This controller provides functionality to list, retrieve and update system configurations.
 */
@RestController
@RequestMapping("/api/system-configs/")
public class SystemConfigController {
    private final ScopedAuditService auditService;
    private final SystemConfigService systemConfigService;

    @Autowired
    public SystemConfigController(AuditService auditService, SystemConfigService systemConfigService) {
        this.auditService = auditService.createScopedAuditService(SystemConfigController.class);
        this.systemConfigService = systemConfigService;
    }

    @GetMapping("")
    public Page<SystemConfigResponseDto> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid SystemConfigFilter filter
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

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
    public <T> SystemConfigResponseDto update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String key,
            @Nonnull @RequestBody @Valid SystemConfigRequestDto updateRequest
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var def = systemConfigService
                .getDefinition(key)
                .orElseThrow(() -> ResponseException.notFound("Für den Schlüssel \"" + key + "\" wurde keine Systemkonfiguration gefunden."));

        var entity = updateRequest
                .toEntity(def);

        var config = systemConfigService
                .save(key, entity);

        // Log the action of updating the system configuration
        auditService
                .logAction(user, AuditAction.Update, SystemConfigEntity.class, Map.of(
                        "key", key,
                        "value", updateRequest.value()
                ));

        return SystemConfigResponseDto
                .fromEntity(config, def);
    }
}
