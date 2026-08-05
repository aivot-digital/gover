package de.aivot.gover.backend.config.controllers;

import de.aivot.gover.backend.audit.enums.AuditAction;
import de.aivot.gover.backend.audit.services.AuditService;
import de.aivot.gover.backend.audit.services.ScopedAuditService;
import de.aivot.gover.backend.config.dtos.UserConfigRequestDto;
import de.aivot.gover.backend.config.dtos.UserConfigResponseDto;
import de.aivot.gover.backend.config.entities.UserConfigEntity;
import de.aivot.gover.backend.config.filters.UserConfigFilter;
import de.aivot.gover.backend.config.models.UserConfigDefinition;
import de.aivot.gover.backend.config.permissions.ConfigPermissionProvider;
import de.aivot.gover.backend.config.services.UserConfigService;
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This controller provides functionality to list, retrieve and update user configurations.
 */
@RestController
@RequestMapping("/api/user-configs/")
@Tag(
        name = OpenApiConstants.Tags.UserConfigsName,
        description = OpenApiConstants.Tags.UserConfigDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class UserConfigController {
    private final static String SELF_USER_ID = "self";

    private final ScopedAuditService auditService;
    private final UserConfigService userConfigService;

    private final Map<String, UserConfigDefinition> userConfigDefinitions;
    private final PermissionService permissionService;
    private final UserService userService;

    @Autowired
    public UserConfigController(AuditService auditService,
                                UserConfigService userConfigService,
                                List<UserConfigDefinition> userConfigDefinitions,
                                PermissionService permissionService,
                                UserService userService) {
        this.auditService = auditService.createScopedAuditService(UserConfigController.class, "Nutzerkonfiguration");
        this.userConfigService = userConfigService;
        this.userConfigDefinitions = userConfigDefinitions
                .stream()
                .collect(Collectors.toMap(UserConfigDefinition::getKey, Function.identity()));
        this.permissionService = permissionService;
        this.userService = userService;
    }

    @GetMapping("definitions/")
    @Operation(
            summary = "List User Configuration Definitions",
            description = "Retrieve a list of all user configuration definitions. This endpoint can be used to get metadata about the available user configurations, such as their types, categories and descriptions. This is especially useful for clients to dynamically adapt to available configurations."
    )
    public List<UserConfigDefinition> list() throws ResponseException {
        return userConfigService
                .getUserConfigDefinitions();
    }

    @GetMapping("{userId}/")
    @Operation(
            summary = "List User Configurations",
            description = "Retrieve a paginated list of user configurations for a specific user with optional filtering. " +
                    "If the special userId 'self' is used, the configurations of the authenticated user will be fetched. " +
                    "Without the system-level permission `" + ConfigPermissionProvider.USER_CONFIG_READ +
                    "`, only public configurations of other users are returned."
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

        // Users without read permission may only see public configurations of other users.
        if (!targetUserId.equals(user.getId()) &&
                !permissionService.hasSystemPermission(user.getId(), ConfigPermissionProvider.USER_CONFIG_READ)) {
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

    @PutMapping("{userId}/{key}/")
    @Operation(
            summary = "Update User Configuration",
            description = "Update the value of a specific user configuration identified by its key for a specific user. " +
                    "If the special userId 'self' is used, the configuration of the authenticated user will be updated. " +
                    "Updating another user's configuration requires the system-level permission `" +
                    ConfigPermissionProvider.USER_CONFIG_UPDATE + "`."
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

        if (!userId.equals(user.getId())) {
            permissionService.requireSystemPermission(user.getId(), ConfigPermissionProvider.USER_CONFIG_UPDATE);
        }

        var def = userConfigDefinitions.get(key);

        var config = request
                .toEntity(def)
                .setUserId(userId)
                .setKey(key);

        config = userConfigService.save(key, userId, config);

        // Log the action of updating the user configuration
        auditService
                .create()
                .withUser(user)
                .withAuditAction(
                        AuditAction.Update,
                        UserConfigEntity.class,
                        config.getKey(),
                        "key",
                        Map.of(
                                "userId", userId
                        ))
                .withMessage(
                        "Die Mitarbeiterkonfiguration %s für %s wurde von der Mitarbeiter:in %s aktualisiert.",
                        StringUtils.quote(config.getKey()),
                        StringUtils.quote(config.getUserId()),
                        StringUtils.quote(user.getFullName())
                )
                .log();

        return UserConfigResponseDto
                .fromEntity(config, def);
    }
}
