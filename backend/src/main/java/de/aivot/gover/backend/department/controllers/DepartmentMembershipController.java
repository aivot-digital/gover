package de.aivot.gover.backend.department.controllers;

import de.aivot.gover.backend.audit.enums.AuditAction;
import de.aivot.gover.backend.audit.services.AuditService;
import de.aivot.gover.backend.audit.services.ScopedAuditService;
import de.aivot.gover.backend.department.dtos.DepartmentMembershipCreateRequestDTO;
import de.aivot.gover.backend.department.entities.DepartmentMembershipEntity;
import de.aivot.gover.backend.department.filters.DepartmentMembershipFilter;
import de.aivot.gover.backend.department.permissions.DepartmentPermissionProvider;
import de.aivot.gover.backend.department.services.DepartmentMembershipService;
import de.aivot.gover.backend.exceptions.InvalidUserEMailException;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.mail.services.DepartmentMembershipMailService;
import de.aivot.gover.backend.mail.services.ExceptionMailService;
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

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/department-memberships/")
@Tag(
        name = OpenApiConstants.Tags.DepartmentMembershipsName,
        description = OpenApiConstants.Tags.DepartmentMembershipsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class DepartmentMembershipController {
    private final ScopedAuditService auditService;

    private final DepartmentMembershipService departmentMembershipService;
    private final DepartmentMembershipMailService departmentMembershipMailService;
    private final ExceptionMailService exceptionMailService;
    private final PermissionService permissionService;
    private final UserService userService;

    @Autowired
    public DepartmentMembershipController(AuditService auditService,
                                          DepartmentMembershipService departmentMembershipService,
                                          DepartmentMembershipMailService departmentMembershipMailService,
                                          ExceptionMailService exceptionMailService,
                                          PermissionService permissionService,
                                          UserService userService) {
        this.auditService = auditService.createScopedAuditService(DepartmentMembershipController.class, "Organisationseinheiten");

        this.departmentMembershipService = departmentMembershipService;
        this.departmentMembershipMailService = departmentMembershipMailService;
        this.exceptionMailService = exceptionMailService;
        this.permissionService = permissionService;
        this.userService = userService;
    }

    @GetMapping("")
    @Operation(
            summary = "List department memberships",
            description = "List department memberships with pagination and filtering."
    )
    public Page<DepartmentMembershipEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid DepartmentMembershipFilter filter

    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!permissionService.hasSystemPermission(user.getId(), DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_READ)) {
            if (filter.getDepartmentId() != null) {
                permissionService.requireDepartmentPermission(
                        user.getId(),
                        filter.getDepartmentId(),
                        DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_READ
                );
            } else {
                var accessibleDepartmentIds = permissionService
                        .getDepartmentsWithPermission(user.getId(), DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_READ);

                if (filter.getDepartmentIds() != null) {
                    accessibleDepartmentIds = filter.getDepartmentIds()
                            .stream()
                            .filter(accessibleDepartmentIds::contains)
                            .toList();
                }

                if (accessibleDepartmentIds.isEmpty()) {
                    return Page.empty(pageable);
                }

                filter.setDepartmentIds(accessibleDepartmentIds);
            }
        }

        return departmentMembershipService
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create department membership",
            description = "Create a new department membership linking a user to a department. " +
                    "Requires the permission `" + DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_CREATE +
                    "` for the target organisation unit or at system level."
    )
    public DepartmentMembershipEntity create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DepartmentMembershipCreateRequestDTO newMembership
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService.requireDepartmentPermission(
                execUser.getId(),
                newMembership.departmentId(),
                DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_CREATE
        );

        var roleIds = newMembership.roleIdsOrEmpty();

        var createdMembership = departmentMembershipService
                .createWithRoles(newMembership.toEntity(), roleIds);

        auditService.create()
                .withUser(execUser)
                .withAuditAction(
                        AuditAction.Create,
                        DepartmentMembershipEntity.class,
                        createdMembership.getId(),
                        "id",
                        Map.of(
                                "departmentId", createdMembership.getDepartmentId(),
                                "userId", createdMembership.getUserId(),
                                "roleIds", roleIds
                        ))
                .withMessage(
                        "Die Zugehörigkeit mit der ID %s für die Organisationseinheit %s und die Mitarbeiter:in %s wurde von der Mitarbeiter:in %s erstellt.",
                        StringUtils.quote(String.valueOf(createdMembership.getId())),
                        StringUtils.quote(String.valueOf(createdMembership.getDepartmentId())),
                        StringUtils.quote(createdMembership.getUserId()),
                        StringUtils.quote(execUser.getFullName())
                )
                .log();

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
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var membership = departmentMembershipService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        permissionService.requireDepartmentPermission(
                user.getId(),
                membership.getDepartmentId(),
                DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_READ
        );

        return membership;
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update department membership",
            description = "Update an existing department membership. " +
                    "Requires the permission `" + DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_UPDATE +
                    "` for the membership's organisation unit or at system level."
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

        permissionService.requireDepartmentPermission(
                execUser.getId(),
                existingMembership.getDepartmentId(),
                DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_UPDATE
        );

        var savedMembership = departmentMembershipService
                .update(id, updatedMembership);

        auditService.create()
                .withUser(execUser)
                .withAuditAction(
                        AuditAction.Update,
                        DepartmentMembershipEntity.class,
                        savedMembership.getId(),
                        "id",
                        Map.of(
                                "departmentId", savedMembership.getDepartmentId(),
                                "userId", savedMembership.getUserId()
                        ))
                .withMessage(
                        "Die Zugehörigkeit mit der ID %s für die Organisationseinheit %s und die Mitarbeiter:in %s wurde von der Mitarbeiter:in %s aktualisiert.",
                        StringUtils.quote(String.valueOf(savedMembership.getId())),
                        StringUtils.quote(String.valueOf(savedMembership.getDepartmentId())),
                        StringUtils.quote(savedMembership.getUserId()),
                        StringUtils.quote(execUser.getFullName())
                )
                .log(); // TODO: Add Diff

        return savedMembership;
    }

    @DeleteMapping("{id}/")
    @Operation(
            summary = "Delete department membership",
            description = "Delete an existing department membership. " +
                    "Requires the permission `" + DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_DELETE +
                    "` for the membership's organisation unit or at system level."
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

        permissionService.requireDepartmentPermission(
                user.getId(),
                existingMembership.getDepartmentId(),
                DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_DELETE
        );

        var deletedMembership = departmentMembershipService
                .deleteEntity(existingMembership);

        auditService.create()
                .withUser(user)
                .withAuditAction(
                        AuditAction.Delete,
                        DepartmentMembershipEntity.class,
                        deletedMembership.getId(),
                        "id",
                        Map.of(
                                "orgUnitId", deletedMembership.getDepartmentId(),
                                "userId", deletedMembership.getUserId()
                        ))
                .withMessage(
                        "Die Zugehörigkeit mit der ID %s für die Organisationseinheit %s und die Mitarbeiter:in %s wurde von der Mitarbeiter:in %s gelöscht.",
                        StringUtils.quote(String.valueOf(deletedMembership.getId())),
                        StringUtils.quote(String.valueOf(deletedMembership.getDepartmentId())),
                        StringUtils.quote(deletedMembership.getUserId()),
                        StringUtils.quote(user.getFullName())
                )
                .log(); // TODO: Add Diff

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
