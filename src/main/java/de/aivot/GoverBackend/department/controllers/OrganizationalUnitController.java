package de.aivot.GoverBackend.department.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.department.dtos.OrganizationalUnitRequestDTO;
import de.aivot.GoverBackend.department.dtos.OrganizationalUnitResponseDTO;
import de.aivot.GoverBackend.department.entities.OrganizationalUnitEntity;
import de.aivot.GoverBackend.department.filters.OrganizationalUnitFilter;
import de.aivot.GoverBackend.department.repositories.OrganizationalUnitRepository;
import de.aivot.GoverBackend.department.repositories.VOrganizationalUnitMembershipWithPermissionsRepository;
import de.aivot.GoverBackend.department.services.OrganizationalUnitMembershipService;
import de.aivot.GoverBackend.department.services.OrganizationalUnitService;
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
@RequestMapping("/api/organizational-units/")
public class OrganizationalUnitController {
    private final ScopedAuditService auditService;

    private final OrganizationalUnitService organizationalUnitService;
    private final OrganizationalUnitRepository organizationalUnitRepository;
    private final VOrganizationalUnitMembershipWithPermissionsRepository vOrganizationalUnitMembershipWithPermissionsRepository;

    @Autowired
    public OrganizationalUnitController(AuditService auditService,
                                        OrganizationalUnitService organizationalUnitService,
                                        OrganizationalUnitRepository organizationalUnitRepository,
                                        VOrganizationalUnitMembershipWithPermissionsRepository vOrganizationalUnitMembershipWithPermissionsRepository) {
        this.auditService = auditService.createScopedAuditService(OrganizationalUnitController.class);

        this.organizationalUnitService = organizationalUnitService;
        this.organizationalUnitRepository = organizationalUnitRepository;
        this.vOrganizationalUnitMembershipWithPermissionsRepository = vOrganizationalUnitMembershipWithPermissionsRepository;
    }

    @GetMapping("")
    public Page<OrganizationalUnitResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid OrganizationalUnitFilter filter
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return organizationalUnitService
                .list(pageable, filter)
                .map(OrganizationalUnitResponseDTO::fromEntity);
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
    public OrganizationalUnitResponseDTO create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid OrganizationalUnitRequestDTO newDepartment
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var department = newDepartment
                .toEntity();

        var createdDepartment = organizationalUnitService
                .create(department);

        auditService.logAction(user, AuditAction.Create, OrganizationalUnitEntity.class, Map.of(
                "id", createdDepartment.getId(),
                "name", createdDepartment.getName()
        ));

        return OrganizationalUnitResponseDTO
                .fromEntity(createdDepartment);
    }

    /**
     * Retrieve a department by its id.
     *
     * @param id The id of the department.
     * @return The department.
     */
    @GetMapping("{id}/")
    public OrganizationalUnitResponseDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var department = organizationalUnitService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        return OrganizationalUnitResponseDTO
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
    public OrganizationalUnitResponseDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @RequestBody @Valid OrganizationalUnitRequestDTO updateRequest
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!user.getGlobalAdmin()) {
            var hasAccess = vOrganizationalUnitMembershipWithPermissionsRepository
                    .existsByUserIdAndOrganizationalUnitIdAndOrgUnitMemberPermissionEditIsTrue(
                            user.getId(),
                            id
                    );
            if (!hasAccess) {
                throw ResponseException
                        .forbidden("Sie benötigen die globale Rolle „Superadmin“ order „Systemadministrator:in“ order benötigen eine Organisationsrolle mit der Berechtigung „Organisationseinheit bearbeiten“.");
            }
        }

        var entity = organizationalUnitService
                .update(id, updateRequest.toEntity());

        auditService.logAction(user, AuditAction.Update, OrganizationalUnitEntity.class, Map.of(
                "id", entity.getId(),
                "name", entity.getName()
        ));

        return OrganizationalUnitResponseDTO
                .fromEntity(entity);
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

        var dep = organizationalUnitRepository
                .findById(id)
                .orElseThrow(ResponseException::notFound);

        auditService.logAction(user, AuditAction.Delete, OrganizationalUnitEntity.class, Map.of(
                "id", dep.getId(),
                "name", dep.getName()
        ));

        organizationalUnitService.delete(id);
    }
}
