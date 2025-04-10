package de.aivot.GoverBackend.department.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.department.dtos.DepartmentRequestDTO;
import de.aivot.GoverBackend.department.dtos.DepartmentResponseDTO;
import de.aivot.GoverBackend.department.entities.DepartmentEntity;
import de.aivot.GoverBackend.department.entities.DepartmentMembershipEntity;
import de.aivot.GoverBackend.department.filters.DepartmentFilter;
import de.aivot.GoverBackend.department.filters.DepartmentWithMembershipFilter;
import de.aivot.GoverBackend.department.repositories.DepartmentRepository;
import de.aivot.GoverBackend.department.services.DepartmentMembershipService;
import de.aivot.GoverBackend.department.services.DepartmentService;
import de.aivot.GoverBackend.department.services.DepartmentWithMembershipService;
import de.aivot.GoverBackend.enums.UserRole;
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
import java.util.Map;

@RestController
@RequestMapping("/api/departments/")
public class DepartmentController {
    private final ScopedAuditService auditService;

    private final DepartmentService departmentService;
    private final DepartmentWithMembershipService departmentWithMembershipService;
    private final DepartmentMembershipService departmentMembershipService;
    private final DepartmentRepository departmentRepository;

    @Autowired
    public DepartmentController(
            AuditService auditService,
            DepartmentService departmentService,
            DepartmentWithMembershipService departmentWithMembershipService,
            DepartmentMembershipService departmentMembershipService,
            DepartmentRepository departmentRepository
    ) {
        this.auditService = auditService.createScopedAuditService(DepartmentController.class);

        this.departmentService = departmentService;
        this.departmentWithMembershipService = departmentWithMembershipService;
        this.departmentMembershipService = departmentMembershipService;
        this.departmentRepository = departmentRepository;
    }

    @GetMapping("")
    public Page<DepartmentResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid DepartmentWithMembershipFilter filter
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // If a user id is set in the filter, return the departments the user is a member of.
        if (filter.getUserId() != null) {
            return departmentWithMembershipService
                    .list(pageable, filter)
                    .map(DepartmentResponseDTO::fromEntity);
        }

        // If the user is a global admin, return all departments.
        if (user.getGlobalAdmin() || Boolean.TRUE.equals(filter.getIgnoreMemberships())) {
            var depFilter = DepartmentFilter
                    .create()
                    .setId(filter.getDepartmentId())
                    .setName(filter.getDepartmentName());

            return departmentService
                    .list(pageable, depFilter)
                    .map(DepartmentResponseDTO::fromEntity);
        }
        // If the user is not an admin, return only the departments the user is a member of.
        else {

            // If no user id is set in the filter, set the user id to the id of the current user.
            if (filter.getUserId() == null) {
                filter.setUserId(user.getId());
            }

            return departmentWithMembershipService
                    .list(pageable, filter)
                    .map(DepartmentResponseDTO::fromEntity);
        }
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
    public DepartmentResponseDTO create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid DepartmentRequestDTO newDepartment
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var department = newDepartment
                .toEntity();

        var createdDepartment = departmentService.create(department);

        auditService.logAction(user, AuditAction.Create, DepartmentEntity.class, Map.of(
                "id", createdDepartment.getId(),
                "name", createdDepartment.getName()
        ));

        // Create an admin membership for the user who created the department.
        var newMembership = new DepartmentMembershipEntity();
        newMembership.setDepartmentId(createdDepartment.getId());
        newMembership.setUserId(user.getId());
        newMembership.setRole(UserRole.Admin);
        departmentMembershipService.create(newMembership);

        return DepartmentResponseDTO
                .fromEntity(createdDepartment);
    }

    /**
     * Retrieve a department by its id.
     *
     * @param id The id of the department.
     * @return The department.
     */
    @GetMapping("{id}/")
    public DepartmentResponseDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var department = departmentService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        return DepartmentResponseDTO
                .fromEntity(department);
    }

    /**
     * Update a department.
     * Only global admins or department admins can update departments.
     *
     * @param jwt           The JWT of the user.
     * @param id            The id of the department.
     * @param updateRequest The updated department.
     * @return The updated department.
     */
    @PutMapping("{id}/")
    public DepartmentResponseDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @RequestBody @Valid DepartmentRequestDTO updateRequest
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!user.getGlobalAdmin()) {
            if (departmentMembershipService.checkUserNotInDepartment(user, id, UserRole.Admin)) {
                throw new ResponseException(HttpStatus.FORBIDDEN, "Nur globale Administratoren:innen oder Fachbereichsadministrator:innen dürfen Fachbereiche bearbeiten.");
            }
        }

        var department = departmentService
                .update(id, updateRequest.toEntity());

        auditService.logAction(user, AuditAction.Update, DepartmentEntity.class, Map.of(
                "id", department.getId(),
                "name", department.getName()
        ));

        return DepartmentResponseDTO
                .fromEntity(department);
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
