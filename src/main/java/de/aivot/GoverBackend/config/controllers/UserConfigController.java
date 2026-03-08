package de.aivot.GoverBackend.config.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.models.AuditLogPayload;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.config.dtos.UserConfigRequestDto;
import de.aivot.GoverBackend.config.dtos.UserConfigResponseDto;
import de.aivot.GoverBackend.config.entities.UserConfigEntity;
import de.aivot.GoverBackend.config.filters.UserConfigFilter;
import de.aivot.GoverBackend.config.models.UserConfigDefinition;
import de.aivot.GoverBackend.config.services.UserConfigService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.user.services.UserService;
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

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This controller provides functionality to list, retrieve and update user configurations.
 */
@RestController
@RequestMapping("/api/user-configs/{userId}/")
@Tag(
        name = "User Configurations",
        description = "User configurations are key-value pairs that define various settings and preferences for individual users. " +
                "These configurations can be used to customize the behavior of the system for each user."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class UserConfigController {
    private final static String SELF_USER_ID = "self";

    private final ScopedAuditService auditService;
    private final UserConfigService userConfigService;

    private final Map<String, UserConfigDefinition> userConfigDefinitions;
    private final UserService userService;

    @Autowired
    public UserConfigController(AuditService auditService,
                                UserConfigService userConfigService,
                                List<UserConfigDefinition> userConfigDefinitions,
                                UserService userService) {
        this.auditService = auditService.createScopedAuditService(UserConfigController.class, "Nutzerkonfiguration");
        this.userConfigService = userConfigService;
        this.userConfigDefinitions = userConfigDefinitions
                .stream()
                .collect(Collectors.toMap(UserConfigDefinition::getKey, Function.identity()));
        this.userService = userService;
    }

    @GetMapping("")
    @Operation(
            summary = "List User Configurations",
            description = "Retrieve a paginated list of user configurations for a specific user with optional filtering. " +
                    "If the special userId 'self' is used, the configurations of the authenticated user will be fetched. " +
                    "Non system admin users can only see public configurations of other users."
    )
    public Page<UserConfigResponseDto> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid UserConfigFilter filter,
            @Nonnull @PathVariable String userId
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var targetUserId = SELF_USER_ID.equals(userId) ? user.getId() : userId;

        filter.setUserId(targetUserId);

        // If the user is not fetching their own configurations, and the user is not an admin only public configurations are allowed
        if (!userId.equals(user.getId()) && !user.getIsSystemAdmin()) {
            filter.setPublicConfig(true);
        }

        // Fetch the user configurations
        return userConfigService
                .list(pageable, filter)
                .map(ent -> {
                    var def = userConfigDefinitions.get(ent.getKey());
                    try {
                        return UserConfigResponseDto.fromEntity(ent, def);
                    } catch (ResponseException e) {
                        throw new RuntimeException(e); // Should never happen
                    }
                });
    }

    @PutMapping("{key}/")
    @Operation(
            summary = "Update User Configuration",
            description = "Update the value of a specific user configuration identified by its key for a specific user. " +
                    "If the special userId 'self' is used, the configuration of the authenticated user will be updated. " +
                    "Users can update their own configurations, while administrators can update configurations for any user."
    )
    public UserConfigResponseDto update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String userId,
            @Nonnull @PathVariable String key,
            @Nonnull @RequestBody UserConfigRequestDto request
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (userId.equals(SELF_USER_ID)) {
            userId = user.getId();
        }

        // If the user is not updating their own configurations, and the user is not an admin, the action is forbidden
        if (!userId.equals(user.getId()) && !user.getIsSystemAdmin()) {
            throw ResponseException.forbidden("Nur Systemadministrator:innen dürfen die Konfigurationen anderer Benutzer ändern.");
        }

        var def = userConfigDefinitions.get(key);

        var config = request
                .toEntity(def)
                .setUserId(userId)
                .setKey(key);

        config = userConfigService.save(key, userId, config);

        // Log the action of updating the user configuration
        auditService.addAuditEntry(AuditLogPayload
                .create()
                .withUser(user)
                .withAuditAction(
                        AuditAction.Update,
                        UserConfigEntity.class,
                        config.getKey(),
                        "key",
                        Map.of(
                                "userId", userId
                        )));

        return UserConfigResponseDto
                .fromEntity(config, def);
    }
}