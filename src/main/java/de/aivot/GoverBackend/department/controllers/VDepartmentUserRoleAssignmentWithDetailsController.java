package de.aivot.GoverBackend.department.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.department.entities.VDepartmentUserRoleAssignmentWithDetailsEntity;
import de.aivot.GoverBackend.department.filters.VDepartmentMembershipWithPermissionsFilter;
import de.aivot.GoverBackend.department.filters.VDepartmentUserRoleAssignmentWithDetailsFilter;
import de.aivot.GoverBackend.department.services.VDepartmentMembershipWithDetailsService;
import de.aivot.GoverBackend.department.services.VDepartmentMembershipWithPermissionsService;
import de.aivot.GoverBackend.department.services.VDepartmentUserRoleAssignmentWithDetailsService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
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
@RequestMapping("/api/department-user-role-assignments-with-details/")
public class VDepartmentUserRoleAssignmentWithDetailsController {
    private final ScopedAuditService auditService;

    private final VDepartmentUserRoleAssignmentWithDetailsService vDepartmentUserRoleAssignmentWithDetailsService;
    private final UserRoleAssignmentService userRoleAssignmentService;
    private final VDepartmentMembershipWithDetailsService vDepartmentMembershipWithDetailsService;
    private final VDepartmentMembershipWithPermissionsService vDepartmentMembershipWithPermissionsService;

    @Autowired
    public VDepartmentUserRoleAssignmentWithDetailsController(AuditService auditService,
                                                              VDepartmentUserRoleAssignmentWithDetailsService vDepartmentUserRoleAssignmentWithDetailsService,
                                                              UserRoleAssignmentService userRoleAssignmentService, VDepartmentMembershipWithDetailsService vDepartmentMembershipWithDetailsService, VDepartmentMembershipWithPermissionsService vDepartmentMembershipWithPermissionsService) {
        this.auditService = auditService.createScopedAuditService(VDepartmentUserRoleAssignmentWithDetailsController.class);

        this.vDepartmentUserRoleAssignmentWithDetailsService = vDepartmentUserRoleAssignmentWithDetailsService;
        this.userRoleAssignmentService = userRoleAssignmentService;
        this.vDepartmentMembershipWithDetailsService = vDepartmentMembershipWithDetailsService;
        this.vDepartmentMembershipWithPermissionsService = vDepartmentMembershipWithPermissionsService;
    }

    @GetMapping("")
    public Page<VDepartmentUserRoleAssignmentWithDetailsEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid VDepartmentUserRoleAssignmentWithDetailsFilter filter
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return vDepartmentUserRoleAssignmentWithDetailsService.list(pageable, filter);
    }

    @PostMapping("")
    public VDepartmentUserRoleAssignmentWithDetailsEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid UserRoleAssignmentEntity newAssignment
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        if (Objects.isNull(newAssignment.getDepartmentMembershipId())) {
            throw ResponseException.badRequest("Die ID der Abteilungszugehörigkeit muss angegeben werden.");
        }

        var membership = vDepartmentMembershipWithDetailsService
                .retrieve(newAssignment.getDepartmentMembershipId())
                .orElseThrow(ResponseException::badRequest);

        if (!user.getGlobalAdmin()) {
            var filter = VDepartmentMembershipWithPermissionsFilter
                    .create()
                    .setUserId(user.getId())
                    .setDepartmentId(membership.getDepartmentId())
                    .setDepartmentPermissionEdit(true);

            var hasPermissionToEdit = vDepartmentMembershipWithPermissionsService
                    .exists(filter.build());

            if (!hasPermissionToEdit) {
                throw ResponseException
                        .forbidden("Sie benötigen die globale Rolle „Superadmin“ order „Systemadministrator:in“ order benötigen eine Organisationsrolle mit der Berechtigung „Organisationseinheit bearbeiten“.");
            }
        }

        var created = userRoleAssignmentService
                .create(newAssignment);

        auditService
                .logAction(user, AuditAction.Create, UserRoleAssignmentEntity.class, Map.of(
                        "id", created.getId(),
                        "userRoleId", created.getUserRoleId()
                ));

        return vDepartmentUserRoleAssignmentWithDetailsService
                .retrieve(created.getId())
                .orElseThrow(ResponseException::notFound);
    }

    @GetMapping("{id}/")
    public VDepartmentUserRoleAssignmentWithDetailsEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return vDepartmentUserRoleAssignmentWithDetailsService
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
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var entity = userRoleAssignmentService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        if (Objects.isNull(entity.getDepartmentMembershipId())) {
            throw ResponseException.badRequest("Die ID der Abteilungszugehörigkeit muss angegeben werden.");
        }

        var membership = vDepartmentMembershipWithDetailsService
                .retrieve(entity.getDepartmentMembershipId())
                .orElseThrow(ResponseException::badRequest);

        if (!user.getGlobalAdmin()) {
            var filter = VDepartmentMembershipWithPermissionsFilter
                    .create()
                    .setUserId(user.getId())
                    .setDepartmentId(membership.getDepartmentId())
                    .setDepartmentPermissionEdit(true);

            var hasPermissionToEdit = vDepartmentMembershipWithPermissionsService
                    .exists(filter.build());

            if (!hasPermissionToEdit) {
                throw ResponseException
                        .forbidden("Sie benötigen die globale Rolle „Superadmin“ order „Systemadministrator:in“ order benötigen eine Organisationsrolle mit der Berechtigung „Organisationseinheit bearbeiten“.");
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

