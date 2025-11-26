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
import de.aivot.GoverBackend.user.services.UserService;
import jakarta.mail.MessagingException;
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
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/department-memberships/")
public class DepartmentMembershipController {
    private final ScopedAuditService auditService;

    private final DepartmentMembershipService departmentMembershipService;
    private final DepartmentMembershipMailService departmentMembershipMailService;
    private final ExceptionMailService exceptionMailService;
    private final VDepartmentMembershipWithPermissionsService vDepartmentMembershipWithPermissionsService;

    @Autowired
    public DepartmentMembershipController(AuditService auditService,
                                          DepartmentMembershipService departmentMembershipService,
                                          DepartmentMembershipMailService departmentMembershipMailService,
                                          ExceptionMailService exceptionMailService,
                                          VDepartmentMembershipWithPermissionsService vDepartmentMembershipWithPermissionsService) {
        this.auditService = auditService.createScopedAuditService(DepartmentMembershipController.class);

        this.departmentMembershipService = departmentMembershipService;
        this.departmentMembershipMailService = departmentMembershipMailService;
        this.exceptionMailService = exceptionMailService;
        this.vDepartmentMembershipWithPermissionsService = vDepartmentMembershipWithPermissionsService;
    }

    @GetMapping("")
    public Page<DepartmentMembershipEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid DepartmentMembershipFilter filter

    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return departmentMembershipService
                .list(pageable, filter);
    }

    @PostMapping("")
    public DepartmentMembershipEntity create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DepartmentMembershipEntity newMembership
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!user.getGlobalAdmin()) {
            var filter = VDepartmentMembershipWithPermissionsFilter
                    .create()
                    .setUserId(user.getId())
                    .setDepartmentId(newMembership.getDepartmentId())
                    .setDepartmentPermissionEdit(true);

            var hasPermissionToEdit = vDepartmentMembershipWithPermissionsService
                    .exists(filter.build());

            if (!hasPermissionToEdit) {
                throw ResponseException
                        .forbidden("Sie benötigen die globale Rolle „Superadmin“ order „Systemadministrator:in“ order benötigen eine Organisationsrolle mit der Berechtigung „Organisationseinheit bearbeiten“.");
            }
        }

        var createdMembership = departmentMembershipService
                .create(newMembership);

        auditService.logAction(user, AuditAction.Create, DepartmentMembershipEntity.class, Map.of(
                "id", createdMembership.getId(),
                "departmentId", createdMembership.getDepartmentId(),
                "userId", createdMembership.getUserId()
        ));

        if (!user.getId().equals(createdMembership.getUserId())) {
            try {
                departmentMembershipMailService
                        .sendAdded(user, createdMembership);
            } catch (MessagingException | IOException | InvalidUserEMailException e) {
                exceptionMailService
                        .send(e);
            }
        }

        return createdMembership;
    }

    @GetMapping("{id}/")
    public DepartmentMembershipEntity retrieve(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return departmentMembershipService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    public DepartmentMembershipEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @Valid @RequestBody DepartmentMembershipEntity updatedMembership
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Fetch existing membership to get department ID reliably
        var existingMembership = departmentMembershipService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        if (!user.getGlobalAdmin()) {
            var filter = VDepartmentMembershipWithPermissionsFilter
                    .create()
                    .setUserId(user.getId())
                    .setDepartmentId(existingMembership.getDepartmentId())
                    .setDepartmentPermissionEdit(true);

            var hasPermissionToEdit = vDepartmentMembershipWithPermissionsService
                    .exists(filter.build());

            if (!hasPermissionToEdit) {
                throw ResponseException
                        .forbidden("Sie benötigen die globale Rolle „Superadmin“ order „Systemadministrator:in“ order benötigen eine Organisationsrolle mit der Berechtigung „Organisationseinheit bearbeiten“.");
            }
        }

        var savedMembership = departmentMembershipService
                .update(id, updatedMembership);

        auditService.logAction(user, AuditAction.Update, DepartmentMembershipEntity.class, Map.of(
                "id", savedMembership.getId(),
                "departmentId", savedMembership.getDepartmentId(),
                "userId", savedMembership.getUserId()
        ));

        return savedMembership;
    }

    @DeleteMapping("{id}/")
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var existingMembership = departmentMembershipService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        if (!user.getGlobalAdmin()) {
            var filter = VDepartmentMembershipWithPermissionsFilter
                    .create()
                    .setUserId(user.getId())
                    .setDepartmentId(existingMembership.getDepartmentId())
                    .setDepartmentPermissionEdit(true);

            var hasPermissionToEdit = vDepartmentMembershipWithPermissionsService
                    .exists(filter.build());

            if (!hasPermissionToEdit) {
                throw ResponseException
                        .forbidden("Sie benötigen die globale Rolle „Superadmin“ order „Systemadministrator:in“ order benötigen eine Organisationsrolle mit der Berechtigung „Organisationseinheit bearbeiten“.");
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
