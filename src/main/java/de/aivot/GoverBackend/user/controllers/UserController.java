package de.aivot.GoverBackend.user.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.permissions.services.PermissionService;
import de.aivot.GoverBackend.user.dtos.CreateUserRequestDTO;
import de.aivot.GoverBackend.user.dtos.CreateUserResponseDTO;
import de.aivot.GoverBackend.user.dtos.SetPasswordRequestDTO;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.filters.UserFilter;
import de.aivot.GoverBackend.user.permissions.UserPermissionProvider;
import de.aivot.GoverBackend.user.services.KeyCloakApiService;
import de.aivot.GoverBackend.user.services.UserProvisioningService;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.utils.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users/")
@Tag(name = "User Management", description = "APIs for managing users")
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class UserController {
    private final ScopedAuditService auditService;

    private final UserService userService;
    private final UserProvisioningService userProvisioningService;
    private final KeyCloakApiService keyCloakApiService;
    private final PermissionService permissionService;

    @Autowired
    public UserController(
            AuditService auditService,
            UserService userService,
            UserProvisioningService userProvisioningService,
            KeyCloakApiService keyCloakApiService, PermissionService permissionService) {
        this.auditService = auditService
                .createScopedAuditService(UserController.class, "Benutzer");

        this.userService = userService;
        this.userProvisioningService = userProvisioningService;
        this.keyCloakApiService = keyCloakApiService;
        this.permissionService = permissionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Users",
            description = "Retrieve a paginated list of users with optional filtering."
    )
    public Page<UserEntity> list(
            @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid UserFilter filter
    ) throws ResponseException {
        permissionService.testSystemPermission(jwt, UserPermissionProvider.USER_READ);

        return userService
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create User",
            description = "Create a new user in the system. Requires the user.create permission."
    )
    public UserEntity create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid UserEntity newUser
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService.testSystemPermission(execUser.getId(), UserPermissionProvider.USER_CREATE);

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

        auditService
                .create()
                .withUser(execUser)
                .withAuditAction(
                        AuditAction.Create,
                        UserEntity.class,
                        result.getId(),
                        "id",
                        Map.of(
                                "id", result.getId(),
                                "email", result.getEmail()
                        ))
                .withMessage(
                        "Die Mitarbeiter:in mit der ID %s und der E-Mail-Adresse %s wurde von der Mitarbeiter:in %s erstellt.",
                        StringUtils.quote(result.getId()),
                        StringUtils.quote(result.getEmail()),
                        StringUtils.quote(execUser.getFullName())
                )
                .log();

        return result;
    }

    @PostMapping("provision/")
    @Operation(
            summary = "Provision User",
            description = "Create a new user in the system, set initial credentials and optionally send them by mail. Requires the user.create permission."
    )
    public CreateUserResponseDTO provision(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid CreateUserRequestDTO request
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService.testSystemPermission(execUser.getId(), UserPermissionProvider.USER_CREATE);

        CreateUserResponseDTO result;
        try {
            result = userProvisioningService
                    .provision(request);
        } catch (Exception e) {
            if (e instanceof ResponseException re) {
                throw re;
            }
            throw ResponseException.badRequest("Fehler beim Anlegen der Mitarbeiter:in", e);
        }

        auditService
                .create()
                .withUser(execUser)
                .withAuditAction(
                        AuditAction.Create,
                        UserEntity.class,
                        result.user().getId(),
                        "id",
                        Map.of(
                                "id", result.user().getId(),
                                "email", result.user().getEmail(),
                                "initialCredentialsSentByEmail", result.initialCredentialsSentByEmail(),
                                "manualCredentialHandoverRequired", result.initialCredentials() != null
                        ))
                .withMessage(
                        result.initialCredentialsSentByEmail()
                                ? "Die Mitarbeiter:in mit der ID %s und der E-Mail-Adresse %s wurde von der Mitarbeiter:in %s erstellt. Die initialen Zugangsdaten wurden automatisch per E-Mail versendet."
                                : "Die Mitarbeiter:in mit der ID %s und der E-Mail-Adresse %s wurde von der Mitarbeiter:in %s erstellt. Die initialen Zugangsdaten müssen manuell weitergegeben werden.",
                        StringUtils.quote(result.user().getId()),
                        StringUtils.quote(result.user().getEmail()),
                        StringUtils.quote(execUser.getFullName())
                )
                .log();

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
            @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String id
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        testUserPermissionOrSelf(execUser, id, UserPermissionProvider.USER_READ);

        return userService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update User",
            description = "Update the details of an existing user. " +
                    "Requires the user.update permission or the user can update their own information."
    )
    public UserEntity update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestBody @Valid UserEntity newUser
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        testUserPermissionOrSelf(execUser, id, UserPermissionProvider.USER_UPDATE);

        UserEntity result;
        try {
            result = userService
                    .update(id, newUser);
        } catch (Exception e) {
            throw ResponseException.badRequest("Fehler beim Speichern der Mitarbeiter:in", e);
        }

        auditService
                .create()
                .withUser(execUser)
                .withAuditAction(
                        AuditAction.Update,
                        UserEntity.class,
                        result.getId(),
                        "id",
                        Map.of(
                                "id", result.getId(),
                                "email", result.getEmail()
                        ))
                .withMessage(
                        "Die Mitarbeiter:in mit der ID %s und der E-Mail-Adresse %s wurde von der Mitarbeiter:in %s aktualisiert.",
                        StringUtils.quote(result.getId()),
                        StringUtils.quote(result.getEmail()),
                        StringUtils.quote(execUser.getFullName())
                )
                .log();

        return result;
    }

    @DeleteMapping("{id}/")
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String id
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        testUserPermissionOrSelf(execUser, id, UserPermissionProvider.USER_DELETE);

        // Delete the user
        var deletedUser = userService
                .delete(id);

        // Log the action
        auditService
                .create()
                .withUser(execUser)
                .withAuditAction(
                        AuditAction.Delete,
                        UserEntity.class,
                        deletedUser.getId(),
                        "id",
                        Map.of(
                                "id", deletedUser.getId(),
                                "email", deletedUser.getEmail(),
                                "firstName", deletedUser.getFirstName(),
                                "lastName", deletedUser.getLastName()
                        ))
                .withMessage(
                        "Die Mitarbeiter:in mit der ID %s und der E-Mail-Adresse %s wurde von der Mitarbeiter:in %s gelöscht.",
                        StringUtils.quote(deletedUser.getId()),
                        StringUtils.quote(deletedUser.getEmail()),
                        StringUtils.quote(execUser.getFullName())
                )
                .log();
    }

    @PutMapping("{id}/reset-password/")
    @Operation(
            summary = "Reset Password",
            description = "Reset the password of a user. " +
                    "Requires the user.update permission or the user can reset their own password."
    )
    public Map<String, String> resetPassword(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        testUserPermissionOrSelf(execUser, id, UserPermissionProvider.USER_UPDATE);

        var user = userService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        keyCloakApiService
                .triggerPasswordReset(id);

        auditService
                .create()
                .withUser(execUser)
                .withAuditAction(
                        AuditAction.Update,
                        UserEntity.class,
                        user.getId(),
                        "id",
                        Map.of(
                                "id", user.getId(),
                                "email", user.getEmail(),
                                "passwordReset", true
                        ))
                .withMessage(
                        "Für die Mitarbeiter:in mit der ID %s und der E-Mail-Adresse %s wurde von der Mitarbeiter:in %s ein Passwort-Reset ausgelöst.",
                        StringUtils.quote(user.getId()),
                        StringUtils.quote(user.getEmail()),
                        StringUtils.quote(execUser.getFullName())
                )
                .log();

        return Map.of(
                "message", "Password reset triggered for user with ID: " + id
        );
    }

    @PutMapping("{id}/password/")
    @Operation(
            summary = "Update Password",
            description = "Update the password of a user. " +
                    "Requires the user.update permission or the user can update their own password."
    )
    public UserEntity updatePassword(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestBody @Valid SetPasswordRequestDTO passwordRequestDTO
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        testUserPermissionOrSelf(execUser, id, UserPermissionProvider.USER_UPDATE);

        var result = userService
                .updatePassword(id, passwordRequestDTO.password());

        auditService
                .create()
                .withUser(execUser)
                .withAuditAction(
                        AuditAction.Update,
                        UserEntity.class,
                        result.getId(),
                        "id",
                        Map.of(
                                "id", result.getId(),
                                "email", result.getEmail(),
                                "passwordChanged", true
                        ))
                .withMessage(
                        "Das Passwort der Mitarbeiter:in mit der ID %s und der E-Mail-Adresse %s wurde von der Mitarbeiter:in %s geändert.",
                        StringUtils.quote(result.getId()),
                        StringUtils.quote(result.getEmail()),
                        StringUtils.quote(execUser.getFullName())
                )
                .log();

        return result;
    }

    private void testUserPermissionOrSelf(@Nonnull UserEntity execUser,
                                          @Nonnull String targetUserId,
                                          @Nonnull String permission) throws ResponseException {
        if (execUser.getId().equals(targetUserId)) {
            return;
        }

        permissionService.testSystemPermission(execUser.getId(), permission);
    }
}
