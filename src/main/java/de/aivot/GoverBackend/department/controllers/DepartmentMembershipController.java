package de.aivot.GoverBackend.department.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.department.entities.DepartmentMembershipEntity;
import de.aivot.GoverBackend.department.filters.DepartmentMembershipFilter;
import de.aivot.GoverBackend.department.filters.VDepartmentMembershipWithPermissionsFilter;
import de.aivot.GoverBackend.department.services.DepartmentMembershipService;
import de.aivot.GoverBackend.department.services.VDepartmentMembershipWithPermissionsService;
import de.aivot.GoverBackend.exceptions.InvalidUserEMailException;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.mail.services.DepartmentMembershipMailService;
import de.aivot.GoverBackend.mail.services.ExceptionMailService;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.userRoles.data.PermissionLabels;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
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
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/department-memberships/")
@Tag(
        name = "Department Memberships",
        description = "Department Memberships link users to organisational units (departments) within the system. " +
                      "They define which users belong to which departments and what roles or permissions they have within those departments. " +
                      "Managing department memberships is crucial for controlling access to resources and functionalities based on organisational structure."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class DepartmentMembershipController {
    private final ScopedAuditService auditService;

    private final DepartmentMembershipService departmentMembershipService;
    private final DepartmentMembershipMailService departmentMembershipMailService;
    private final ExceptionMailService exceptionMailService;
    private final VDepartmentMembershipWithPermissionsService vDepartmentMembershipWithPermissionsService;
    private final UserService userService;

    @Autowired
    public DepartmentMembershipController(AuditService auditService,
                                          DepartmentMembershipService departmentMembershipService,
                                          DepartmentMembershipMailService departmentMembershipMailService,
                                          ExceptionMailService exceptionMailService,
                                          VDepartmentMembershipWithPermissionsService vDepartmentMembershipWithPermissionsService,
                                          UserService userService) {
        this.auditService = auditService.createScopedAuditService(DepartmentMembershipController.class);

        this.departmentMembershipService = departmentMembershipService;
        this.departmentMembershipMailService = departmentMembershipMailService;
        this.exceptionMailService = exceptionMailService;
        this.vDepartmentMembershipWithPermissionsService = vDepartmentMembershipWithPermissionsService;
        this.userService = userService;
    }

    @GetMapping("")
    @Operation(
            summary = "List department memberships",
            description = "List department memberships with pagination and filtering."
    )
    public Page<DepartmentMembershipEntity> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid DepartmentMembershipFilter filter

    ) throws ResponseException {
        return departmentMembershipService
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create department membership",
            description = "Create a new department membership linking a user to a department. " +
                          "Requires super admin permissions or department edit permissions for the membership's target department."
    )
    public DepartmentMembershipEntity create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DepartmentMembershipEntity newMembership
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!execUser.getIsSuperAdmin()) {
            var filter = VDepartmentMembershipWithPermissionsFilter
                    .create()
                    .setUserId(execUser.getId())
                    .setDepartmentId(newMembership.getDepartmentId())
                    .setDepartmentPermissionEdit(true);

            var hasPermissionToEdit = vDepartmentMembershipWithPermissionsService
                    .exists(filter.build());

            if (!hasPermissionToEdit) {
                throw ResponseException
                        .noPermission(PermissionLabels.DepartmentPermissionEdit);
            }
        }

        var createdMembership = departmentMembershipService
                .create(newMembership);

        auditService.logAction(execUser, AuditAction.Create, DepartmentMembershipEntity.class, Map.of(
                "id", createdMembership.getId(),
                "departmentId", createdMembership.getDepartmentId(),
                "userId", createdMembership.getUserId()
        ));

        if (!execUser.getId().equals(createdMembership.getUserId())) {
            try {
                departmentMembershipMailService
                        .sendAdded(execUser, createdMembership);
            } catch (MessagingException | IOException | InvalidUserEMailException e) {
                exceptionMailService
                        .send(e);
            }
        }

        return createdMembership;
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve department membership",
            description = "Retrieve a department membership by its id."
    )
    public DepartmentMembershipEntity retrieve(
            @PathVariable Integer id
    ) throws ResponseException {
        return departmentMembershipService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update department membership",
            description = "Update an existing department membership. " +
                          "Requires super admin permissions or department edit permissions for the membership's department."
    )
    public DepartmentMembershipEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @Valid @RequestBody DepartmentMembershipEntity updatedMembership
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Fetch existing membership to get department ID reliably
        var existingMembership = departmentMembershipService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        if (!execUser.getIsSuperAdmin()) {
            var filter = VDepartmentMembershipWithPermissionsFilter
                    .create()
                    .setUserId(execUser.getId())
                    .setDepartmentId(existingMembership.getDepartmentId())
                    .setDepartmentPermissionEdit(true);

            var hasPermissionToEdit = vDepartmentMembershipWithPermissionsService
                    .exists(filter.build());

            if (!hasPermissionToEdit) {
                throw ResponseException
                        .noPermission(PermissionLabels.DepartmentPermissionEdit);
            }
        }

        var savedMembership = departmentMembershipService
                .update(id, updatedMembership);

        auditService.logAction(execUser, AuditAction.Update, DepartmentMembershipEntity.class, Map.of(
                "id", savedMembership.getId(),
                "departmentId", savedMembership.getDepartmentId(),
                "userId", savedMembership.getUserId()
        ));

        return savedMembership;
    }

    @DeleteMapping("{id}/")
    @Operation(
            summary = "Delete department membership",
            description = "Delete an existing department membership. " +
                          "Requires super admin permissions or department edit permissions for the membership's department."
    )
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var existingMembership = departmentMembershipService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        if (!user.getIsSuperAdmin()) {
            var filter = VDepartmentMembershipWithPermissionsFilter
                    .create()
                    .setUserId(user.getId())
                    .setDepartmentId(existingMembership.getDepartmentId())
                    .setDepartmentPermissionEdit(true);

            var hasPermissionToEdit = vDepartmentMembershipWithPermissionsService
                    .exists(filter.build());

            if (!hasPermissionToEdit) {
                throw ResponseException
                        .noPermission(PermissionLabels.DepartmentPermissionEdit);
            }
        }

        var deletedMembership = departmentMembershipService
                .deleteEntity(existingMembership);

        auditService.logAction(user, AuditAction.Delete, DepartmentMembershipEntity.class, Map.of(
                "id", deletedMembership.getId(),
                "orgUnitId", deletedMembership.getDepartmentId(),
                "userId", deletedMembership.getUserId()
        ));

        if (!user.getId().equals(deletedMembership.getUserId())) {
            try {
                departmentMembershipMailService
                        .sendRemoved(user, deletedMembership);
            } catch (MessagingException | IOException | InvalidUserEMailException e) {
                exceptionMailService
                        .send(e);
            }
        }
    }
}
