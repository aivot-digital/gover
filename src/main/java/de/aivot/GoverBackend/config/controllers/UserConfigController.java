package de.aivot.GoverBackend.config.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.config.dtos.UserConfigRequestDto;
import de.aivot.GoverBackend.config.dtos.UserConfigResponseDto;
import de.aivot.GoverBackend.config.entities.UserConfigEntity;
import de.aivot.GoverBackend.config.entities.UserConfigEntityId;
import de.aivot.GoverBackend.config.filters.UserConfigFilter;
import de.aivot.GoverBackend.config.models.UserConfigDefinition;
import de.aivot.GoverBackend.config.services.UserConfigService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.user.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This controller provides functionality to list, retrieve and update user configurations.
 */
@RestController
@RequestMapping("/api/user-configs/{userId}/")
public class UserConfigController {
    private final static String SELF_USER_ID = "self";

    private final ScopedAuditService auditService;
    private final UserConfigService userConfigService;

    private final Map<String, UserConfigDefinition> userConfigDefinitions;

    @Autowired
    public UserConfigController(AuditService auditService, UserConfigService userConfigService, List<UserConfigDefinition> userConfigDefinitions) {
        this.auditService = auditService.createScopedAuditService(UserConfigController.class);
        this.userConfigService = userConfigService;
        this.userConfigDefinitions = userConfigDefinitions
                .stream()
                .collect(Collectors.toMap(UserConfigDefinition::getKey, Function.identity()));
    }

    @GetMapping("")
    public Page<UserConfigResponseDto> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid UserConfigFilter filter,
            @Nonnull @PathVariable String userId
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var targetUserId = SELF_USER_ID.equals(userId) ? user.getId() : userId;

        filter.setUserId(targetUserId);

        // If the user is not fetching their own configurations, and the user is not an admin only public configurations are allowed
        if (!userId.equals(user.getId()) && !user.getGlobalAdmin()) {
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
    public UserConfigResponseDto update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String userId,
            @Nonnull @PathVariable String key,
            @Nonnull @RequestBody UserConfigRequestDto request
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (userId.equals(SELF_USER_ID)) {
            userId = user.getId();
        }

        // If the user is not updating their own configurations, and the user is not an admin, the action is forbidden
        if (!userId.equals(user.getId()) && !user.getGlobalAdmin()) {
            throw new ResponseException(HttpStatus.FORBIDDEN, "Nur Administratoren dürfen die Konfigurationen anderer Benutzer ändern.");
        }

        var def = userConfigDefinitions.get(key);

        var config = request
                .toEntity(def)
                .setUserId(userId)
                .setKey(key);

        config = userConfigService.save(key, userId, config);

        // Log the action of updating the user configuration
        auditService
                .logAction(user, AuditAction.Update, UserConfigEntity.class, Map.of(
                        "userId", userId,
                        "key", key,
                        "value", request.value()
                ));

        return UserConfigResponseDto
                .fromEntity(config, def);
    }
}