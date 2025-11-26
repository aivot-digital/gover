package de.aivot.GoverBackend.department.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.department.entities.DepartmentEntity;
import de.aivot.GoverBackend.department.filters.DepartmentFilter;
import de.aivot.GoverBackend.department.filters.VDepartmentMembershipWithPermissionsFilter;
import de.aivot.GoverBackend.department.repositories.DepartmentRepository;
import de.aivot.GoverBackend.department.services.DepartmentService;
import de.aivot.GoverBackend.department.services.VDepartmentMembershipWithPermissionsService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
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
@RequestMapping("/api/departments/")
public class DepartmentController {
    private final ScopedAuditService auditService;

    private final DepartmentService departmentService;
    private final DepartmentRepository departmentRepository;
    private final VDepartmentMembershipWithPermissionsService vDepartmentMembershipWithPermissionsService;

    @Autowired
    public DepartmentController(AuditService auditService,
                                DepartmentService departmentService,
                                DepartmentRepository departmentRepository,
                                VDepartmentMembershipWithPermissionsService vDepartmentMembershipWithPermissionsService) {
        this.auditService = auditService.createScopedAuditService(DepartmentController.class);

        this.departmentService = departmentService;
        this.departmentRepository = departmentRepository;
        this.vDepartmentMembershipWithPermissionsService = vDepartmentMembershipWithPermissionsService;
    }

    @GetMapping("")
    public Page<DepartmentEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid DepartmentFilter filter
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return departmentService
                .list(pageable, filter);
    }

    /**
     * Create a new department.
     * Only global admins can create departments.
     *
     * @param jwt           The JWT of the user.
     * @param newDepartment The new department.
     * @return The created department.
     */
    @PostMapping("")
    public DepartmentEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid DepartmentEntity newDepartment
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var createdDepartment = departmentService
                .create(newDepartment);

        auditService.logAction(user, AuditAction.Create, DepartmentEntity.class, Map.of(
                "id", createdDepartment.getId(),
                "name", createdDepartment.getName()
        ));

        return createdDepartment;
    }

    /**
     * Retrieve a department by its id.
     *
     * @param id The id of the department.
     * @return The department.
     */
    @GetMapping("{id}/")
    public DepartmentEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return departmentService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }

    /**
     * Update a department.
     * Only global admins or department admins can update departments.
     *
     * @param jwt               The JWT of the user.
     * @param id                The id of the department.
     * @param updatedDepartment The updated department.
     * @return The updated department.
     */
    @PutMapping("{id}/")
    public DepartmentEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @RequestBody @Valid DepartmentEntity updatedDepartment
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!user.getGlobalAdmin()) {
            var filter = VDepartmentMembershipWithPermissionsFilter
                    .create()
                    .setUserId(user.getId())
                    .setDepartmentId(id)
                    .setDepartmentPermissionEdit(true);

            var hasPermissionToEdit = vDepartmentMembershipWithPermissionsService
                    .exists(filter.build());

            if (!hasPermissionToEdit) {
                throw ResponseException
                        .forbidden("Sie benötigen die globale Rolle „Superadmin“ order „Systemadministrator:in“ order benötigen eine Organisationsrolle mit der Berechtigung „Organisationseinheit bearbeiten“.");
            }
        }

        var savedDepartment = departmentService
                .update(id, updatedDepartment);

        auditService.logAction(user, AuditAction.Update, DepartmentEntity.class, Map.of(
                "id", savedDepartment.getId(),
                "name", savedDepartment.getName()
        ));

        return savedDepartment;
    }

    /**
     * Delete a department.
     * Only global admins can delete departments.
     *
     * @param jwt The JWT of the user.
     * @param id  The id of the department.
     */
    @DeleteMapping("{id}/")
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var dep = departmentRepository
                .findById(id)
                .orElseThrow(ResponseException::notFound);

        auditService.logAction(user, AuditAction.Delete, DepartmentEntity.class, Map.of(
                "id", dep.getId(),
                "name", dep.getName()
        ));

        departmentService.delete(id);
    }
}
