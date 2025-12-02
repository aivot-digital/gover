package de.aivot.GoverBackend.teams.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.teams.entities.VTeamUserRoleAssignmentWithDetailsEntity;
import de.aivot.GoverBackend.teams.filters.VTeamMembershipWithPermissionsFilter;
import de.aivot.GoverBackend.teams.filters.VTeamUserRoleAssignmentWithDetailsFilter;
import de.aivot.GoverBackend.teams.services.VTeamMembershipWithDetailsService;
import de.aivot.GoverBackend.teams.services.VTeamMembershipWithPermissionsService;
import de.aivot.GoverBackend.teams.services.VTeamUserRoleAssignmentWithDetailsService;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.userRoles.entities.UserRoleAssignmentEntity;
import de.aivot.GoverBackend.userRoles.services.UserRoleAssignmentService;
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
import java.util.Objects;

@RestController
@RequestMapping("/api/team-user-role-assignments-with-details/")
public class VTeamUserRoleAssignmentWithDetailsController {
    private final ScopedAuditService auditService;

    private final VTeamUserRoleAssignmentWithDetailsService VTeamUserRoleAssignmentWithDetailsService;
    private final UserRoleAssignmentService userRoleAssignmentService;
    private final VTeamMembershipWithDetailsService VTeamMembershipWithDetailsService;
    private final VTeamMembershipWithPermissionsService vTeamMembershipWithPermissionsService;

    @Autowired
    public VTeamUserRoleAssignmentWithDetailsController(AuditService auditService,
                                                        VTeamUserRoleAssignmentWithDetailsService VTeamUserRoleAssignmentWithDetailsService,
                                                        UserRoleAssignmentService userRoleAssignmentService,
                                                        VTeamMembershipWithDetailsService VTeamMembershipWithDetailsService,
                                                        VTeamMembershipWithPermissionsService vTeamMembershipWithPermissionsService) {
        this.auditService = auditService.createScopedAuditService(VTeamUserRoleAssignmentWithDetailsController.class);

        this.VTeamUserRoleAssignmentWithDetailsService = VTeamUserRoleAssignmentWithDetailsService;
        this.userRoleAssignmentService = userRoleAssignmentService;
        this.VTeamMembershipWithDetailsService = VTeamMembershipWithDetailsService;
        this.vTeamMembershipWithPermissionsService = vTeamMembershipWithPermissionsService;
    }

    @GetMapping("")
    public Page<VTeamUserRoleAssignmentWithDetailsEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid VTeamUserRoleAssignmentWithDetailsFilter filter
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return VTeamUserRoleAssignmentWithDetailsService.list(pageable, filter);
    }

    @PostMapping("")
    public VTeamUserRoleAssignmentWithDetailsEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid UserRoleAssignmentEntity newAssignment
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asGlobalAdmin()
                .orElseThrow(ResponseException::forbidden);

        if (Objects.isNull(newAssignment.getDepartmentMembershipId())) {
            throw ResponseException.badRequest("Die ID der Teamzugehörigkeit muss angegeben werden.");
        }

        var membership = VTeamMembershipWithDetailsService
                .retrieve(newAssignment.getDepartmentMembershipId())
                .orElseThrow(ResponseException::badRequest);

        if (!user.getSuperAdmin()) {
            var filter = VTeamMembershipWithPermissionsFilter
                    .create()
                    .setUserId(user.getId())
                    .setTeamId(membership.getTeamId())
                    .setTeamPermissionEdit(true);

            var hasPermissionToEdit = vTeamMembershipWithPermissionsService
                    .exists(filter.build());

            if (!hasPermissionToEdit) {
                throw ResponseException
                        .forbidden("Sie benötigen die globale Rolle „Superadmin“ order „Systemadministrator:in“ order benötigen eine Organisationsrolle mit der Berechtigung „Team bearbeiten“.");
            }
        }

        var created = userRoleAssignmentService
                .create(newAssignment);

        auditService
                .logAction(user, AuditAction.Create, UserRoleAssignmentEntity.class, Map.of(
                        "id", created.getId(),
                        "userRoleId", created.getUserRoleId()
                ));

        return VTeamUserRoleAssignmentWithDetailsService
                .retrieve(created.getId())
                .orElseThrow(ResponseException::notFound);
    }

    @GetMapping("{id}/")
    public VTeamUserRoleAssignmentWithDetailsEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return VTeamUserRoleAssignmentWithDetailsService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }

    @DeleteMapping("{id}/")
    public void destroy(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asGlobalAdmin()
                .orElseThrow(ResponseException::forbidden);

        var entity = userRoleAssignmentService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        if (Objects.isNull(entity.getDepartmentMembershipId())) {
            throw ResponseException.badRequest("Die ID der Teamzugehörigkeit muss angegeben werden.");
        }

        var membership = VTeamMembershipWithDetailsService
                .retrieve(entity.getDepartmentMembershipId())
                .orElseThrow(ResponseException::badRequest);

        if (!user.getSuperAdmin()) {
            var filter = VTeamMembershipWithPermissionsFilter
                    .create()
                    .setUserId(user.getId())
                    .setTeamId(membership.getTeamId())
                    .setTeamPermissionEdit(true);

            var hasPermissionToEdit = vTeamMembershipWithPermissionsService
                    .exists(filter.build());

            if (!hasPermissionToEdit) {
                throw ResponseException
                        .forbidden("Sie benötigen die globale Rolle „Superadmin“ order „Systemadministrator:in“ order benötigen eine Organisationsrolle mit der Berechtigung „Team bearbeiten“.");
            }
        }

        userRoleAssignmentService
                .deleteEntity(entity);

        auditService
                .logAction(user, AuditAction.Delete, UserRoleAssignmentEntity.class, Map.of(
                        "id", entity.getId(),
                        "userRoleId", entity.getUserRoleId()
                ));
    }
}
