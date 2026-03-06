package de.aivot.GoverBackend.user.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.user.dtos.SetPasswordRequestDTO;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.filters.UserFilter;
import de.aivot.GoverBackend.user.services.KeyCloakApiService;
import de.aivot.GoverBackend.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/users/")
@Tag(name = "User Management", description = "APIs for managing users")
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class UserController {
    private final ScopedAuditService auditService;

    private final UserService userService;
    private final KeyCloakApiService keyCloakApiService;

    @Autowired
    public UserController(
            AuditService auditService,
            UserService userService,
            KeyCloakApiService keyCloakApiService) {
        this.auditService = auditService
                .createScopedAuditService(UserController.class);

        this.userService = userService;
        this.keyCloakApiService = keyCloakApiService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Users",
            description = "Retrieve a paginated list of users with optional filtering."
    )
    public Page<UserEntity> list(
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid UserFilter filter
    ) throws ResponseException {
        return userService
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create User",
            description = "Create a new user in the system. Requires super admin permissions."
    )
    public UserEntity create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid UserEntity newUser
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSuperAdmin()
                .orElseThrow(ResponseException::noSuperAdminPermission);

        UserEntity result;
        try {
            result = userService
                    .create(newUser);
        } catch (Exception e) {
            if (e instanceof ResponseException re) {
                throw re;
            }
            throw ResponseException.badRequest("Fehler beim Anlegen der Mitarbeiter:in", e);
        }

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.ofLegacyAction(execUser, AuditAction.Create, UserEntity.class, Map.of(
                "id", result.getId(),
                "email", result.getEmail()
        )));

        return result;
    }

    @GetMapping("self/")
    @Operation(
            summary = "Retrieve Self",
            description = "Retrieve the details of the currently authenticated user. " +
                    "If the user does not exist in the system, the authenticated user's information is imported from Keycloak."
    )
    public UserEntity retrieveSelf(
            @Nullable @AuthenticationPrincipal Jwt jwt
    ) throws ResponseException {
        return userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve User",
            description = "Retrieve the details of a user by their ID."
    )
    public UserEntity retrieve(
            @Nonnull @PathVariable String id
    ) throws ResponseException {
        return userService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update User",
            description = "Update the details of an existing user. " +
                    "Requires super admin permissions or the user can update their own information."
    )
    public UserEntity update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestBody @Valid UserEntity newUser
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!execUser.getIsSuperAdmin() && !execUser.getId().equals(id)) {
            throw ResponseException.noSuperAdminPermission();
        }

        UserEntity result;
        try {
            result = userService
                    .update(id, newUser);
        } catch (Exception e) {
            throw ResponseException.badRequest("Fehler beim Speichern der Mitarbeiter:in", e);
        }

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.ofLegacyAction(execUser, AuditAction.Update, UserEntity.class, Map.of(
                "id", result.getId(),
                "email", result.getEmail()
        )));

        return result;
    }

    @DeleteMapping("{id}/")
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSuperAdmin()
                .orElseThrow(ResponseException::noSuperAdminPermission);

        // Delete the user
        var deletedUser = userService
                .delete(id);

        // Log the action
        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.ofLegacyAction(
                        user,
                        AuditAction.Delete,
                        UserEntity.class,
                        Map.of(
                                "id", deletedUser.getId(),
                                "email", deletedUser.getEmail(),
                                "firstName", deletedUser.getFirstName(),
                                "lastName", deletedUser.getLastName()
                        )
                ));
    }

    @PutMapping("{id}/reset-password/")
    @Operation(
            summary = "Reset Password",
            description = "Reset the password of a user. " +
                    "Requires super admin permissions or the user can reset their own password."
    )
    public Map<String, String> resetPassword(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!execUser.getIsSuperAdmin() && !execUser.getId().equals(id)) {
            throw ResponseException.noSuperAdminPermission();
        }

        var user = userService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        keyCloakApiService
                .triggerPasswordReset(id);

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.ofLegacyAction(execUser, AuditAction.Update, UserEntity.class, Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "passwordReset", true
        )));

        return Map.of(
                "message", "Password reset triggered for user with ID: " + id
        );
    }

    @PutMapping("{id}/password/")
    @Operation(
            summary = "Update Password",
            description = "Update the password of a user. " +
                    "Requires super admin permissions or the user can update their own password."
    )
    public UserEntity updatePassword(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestBody @Valid SetPasswordRequestDTO passwordRequestDTO
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!execUser.getIsSuperAdmin() && !execUser.getId().equals(id)) {
            throw ResponseException.noSuperAdminPermission();
        }

        var result = userService
                .updatePassword(id, passwordRequestDTO.password());

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.ofLegacyAction(execUser, AuditAction.Update, UserEntity.class, Map.of(
                "id", result.getId(),
                "email", result.getEmail(),
                "passwordChanged", true
        )));

        return result;
    }
}
