package de.aivot.GoverBackend.user.controllers;

import de.aivot.GoverBackend.asset.filters.AssetFilter;
import de.aivot.GoverBackend.asset.services.AssetService;
import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.submission.filters.SubmissionFilter;
import de.aivot.GoverBackend.submission.services.SubmissionService;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.filters.UserFilter;
import de.aivot.GoverBackend.user.repositories.UserRepository;
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

@RestController
@RequestMapping("/api/users/")
public class UserController {
    private final ScopedAuditService auditService;

    private final UserService userService;
    private final UserRepository userRepository;
    private final SubmissionService submissionService;
    private final AssetService assetService;

    @Autowired
    public UserController(
            UserService userService,
            UserRepository userRepository,
            SubmissionService submissionService,
            AssetService assetService,
            AuditService auditService
    ) {
        this.auditService = auditService.createScopedAuditService(UserController.class);

        this.userService = userService;
        this.userRepository = userRepository;
        this.submissionService = submissionService;
        this.assetService = assetService;
    }

    @GetMapping("")
    public Page<UserEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid UserFilter filter
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return userService
                .list(pageable, filter);
    }

    @GetMapping("self/")
    public UserEntity retrieveSelf(
            @Nullable @AuthenticationPrincipal Jwt jwt
    ) throws ResponseException {
        var jwtUser = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return userService
                .retrieveUserFromKeycloak(jwtUser.getId())
                .orElseThrow(ResponseException::notFound);
    }

    @GetMapping("{id}/")
    public UserEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String id
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return userService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }

    @DeleteMapping("{id}/")
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String id
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        // Retrieve the user to delete
        var userToDelete = userService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        // Check if the user is deleted in the idp. Only users already deleted in the idp can be deleted.
        if (userToDelete.getDeletedInIdp() == null || !userToDelete.getDeletedInIdp()) {
            throw ResponseException.conflict("Die Mitarbeiter:in wurde noch nicht im IDP gelöscht.");
        }

        // Check if the user is assigned to any submissions
        var hasAssignments = submissionService
                .exists(SubmissionFilter.create().setAssigneeId(userToDelete.getId()));
        if (hasAssignments) {
            throw ResponseException.conflict("Die Mitarbeiter:in ist noch Aufträgen zugewiesen.");
        }

        // Check if the user has uploaded any assets
        var hasAssets = assetService
                .exists(AssetFilter.create().setUploaderId(userToDelete.getId()));
        if (hasAssets) {
            throw ResponseException.conflict("Die Mitarbeiter:in hat noch Assets hochgeladen.");
        }

        // Delete the user
        userRepository
                .delete(userToDelete);

        // Log the action
        auditService
                .logAction(
                        user,
                        AuditAction.Delete,
                        UserEntity.class,
                        Map.of(
                                "id", userToDelete.getId(),
                                "email", userToDelete.getEmail(),
                                "firstName", userToDelete.getFirstName(),
                                "lastName", userToDelete.getLastName()
                        )
                );
    }
}
