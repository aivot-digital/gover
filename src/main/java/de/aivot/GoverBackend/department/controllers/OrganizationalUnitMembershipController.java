package de.aivot.GoverBackend.department.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.department.dtos.DepartmentMembershipRequestDTO;
import de.aivot.GoverBackend.department.dtos.DepartmentMembershipResponseDTO;
import de.aivot.GoverBackend.department.entities.OrganizationalUnitMembershipEntity;
import de.aivot.GoverBackend.department.filters.OrganizationalUnitMembershipFilter;
import de.aivot.GoverBackend.department.filters.VOrganizationalUnitMembershipWithDetailsFilter;
import de.aivot.GoverBackend.department.repositories.VOrganizationalUnitMembershipWithPermissionsRepository;
import de.aivot.GoverBackend.department.services.OrganizationalUnitMembershipService;
import de.aivot.GoverBackend.department.services.OrganizationalUnitService;
import de.aivot.GoverBackend.department.services.VOrganizationalUnitMembershipWithDetailsService;
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
@RequestMapping("/api/organizational-unit-memberships/")
public class OrganizationalUnitMembershipController {
    private final ScopedAuditService auditService;

    private final OrganizationalUnitMembershipService organizationalUnitMembershipService;
    private final DepartmentMembershipMailService departmentMembershipMailService;
    private final ExceptionMailService exceptionMailService;
    private final UserService userService;
    private final OrganizationalUnitService organizationalUnitService;
    private final VOrganizationalUnitMembershipWithDetailsService vOrganizationalUnitMembershipWithDetailsService;
    private final VOrganizationalUnitMembershipWithPermissionsRepository vOrganizationalUnitMembershipWithPermissionsRepository;

    @Autowired
    public OrganizationalUnitMembershipController(
            AuditService auditService,
            OrganizationalUnitMembershipService organizationalUnitMembershipService,
            DepartmentMembershipMailService departmentMembershipMailService,
            ExceptionMailService exceptionMailService,
            UserService userService,
            OrganizationalUnitService organizationalUnitService,
            VOrganizationalUnitMembershipWithDetailsService vOrganizationalUnitMembershipWithDetailsService, VOrganizationalUnitMembershipWithPermissionsRepository vOrganizationalUnitMembershipWithPermissionsRepository) {
        this.auditService = auditService.createScopedAuditService(OrganizationalUnitMembershipController.class);

        this.organizationalUnitMembershipService = organizationalUnitMembershipService;
        this.departmentMembershipMailService = departmentMembershipMailService;
        this.exceptionMailService = exceptionMailService;
        this.userService = userService;
        this.organizationalUnitService = organizationalUnitService;
        this.vOrganizationalUnitMembershipWithDetailsService = vOrganizationalUnitMembershipWithDetailsService;
        this.vOrganizationalUnitMembershipWithPermissionsRepository = vOrganizationalUnitMembershipWithPermissionsRepository;
    }

    @GetMapping("")
    public Page<DepartmentMembershipResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid VOrganizationalUnitMembershipWithDetailsFilter filter

    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return vOrganizationalUnitMembershipWithDetailsService
                .list(pageable, filter)
                .map(DepartmentMembershipResponseDTO::fromEntity);
    }

    @PostMapping("")
    public DepartmentMembershipResponseDTO create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DepartmentMembershipRequestDTO requestDTO
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!user.getGlobalAdmin()) {
            var hasAccess = vOrganizationalUnitMembershipWithPermissionsRepository
                    .existsByUserIdAndOrganizationalUnitIdAndOrgUnitMemberPermissionEditIsTrue(
                            user.getId(),
                            requestDTO.organizationalUnitId()
                    );

            if (!hasAccess) {
                throw ResponseException
                        .forbidden("Sie benötigen die globale Rolle „Superadmin“ order „Systemadministrator:in“ order benötigen eine Organisationsrolle mit der Berechtigung „Organisationseinheit bearbeiten“.");
            }
        }

        var targetUser = userService
                .retrieve(requestDTO.userId())
                .orElseThrow(ResponseException::badRequest);

        var department = organizationalUnitService
                .retrieve(requestDTO.organizationalUnitId())
                .orElseThrow(ResponseException::badRequest);

        var departmentMembership = organizationalUnitMembershipService
                .create(requestDTO.toEntity());

        auditService.logAction(user, AuditAction.Create, OrganizationalUnitMembershipEntity.class, Map.of(
                "id", departmentMembership.getId(),
                "orgUnitId", departmentMembership.getOrganizationalUnitId(),
                "userId", departmentMembership.getUserId()
        ));

        if (!user.getId().equals(departmentMembership.getUserId())) {
            try {
                departmentMembershipMailService
                        .sendAdded(user, departmentMembership);
            } catch (MessagingException | IOException | InvalidUserEMailException e) {
                exceptionMailService
                        .send(e);
            }
        }

        return new DepartmentMembershipResponseDTO(
                departmentMembership.getId(),
                departmentMembership.getOrganizationalUnitId(),
                department.getName(),
                departmentMembership.getUserId(),
                targetUser.getFullName(),
                targetUser.getEmail()
        );
    }

    @GetMapping("{id}/")
    public DepartmentMembershipResponseDTO retrieve(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var mem = organizationalUnitMembershipService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        var department = organizationalUnitService
                .retrieve(mem.getOrganizationalUnitId())
                .orElseThrow(ResponseException::notFound);

        var user = userService
                .retrieve(mem.getUserId())
                .orElseThrow(ResponseException::notFound);

        return DepartmentMembershipResponseDTO
                .fromEntity(mem, department, user);
    }

    @PutMapping("{id}/")
    public DepartmentMembershipResponseDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @Valid @RequestBody DepartmentMembershipRequestDTO requestDTO
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!user.getGlobalAdmin()) {
            var hasAccess = vOrganizationalUnitMembershipWithPermissionsRepository
                    .existsByUserIdAndOrganizationalUnitIdAndOrgUnitMemberPermissionEditIsTrue(
                            user.getId(),
                            requestDTO.organizationalUnitId()
                    );

            if (!hasAccess) {
                throw ResponseException
                        .forbidden("Sie benötigen die globale Rolle „Superadmin“ order „Systemadministrator:in“ order benötigen eine Organisationsrolle mit der Berechtigung „Organisationseinheit bearbeiten“.");
            }
        }

        var targetUser = userService
                .retrieve(requestDTO.userId())
                .orElseThrow(ResponseException::notFound);

        var department = organizationalUnitService
                .retrieve(requestDTO.organizationalUnitId())
                .orElseThrow(ResponseException::notFound);

        var updatedMembership = organizationalUnitMembershipService
                .update(id, requestDTO.toEntity());

        return new DepartmentMembershipResponseDTO(
                updatedMembership.getId(),
                updatedMembership.getOrganizationalUnitId(),
                department.getName(),
                updatedMembership.getUserId(),
                targetUser.getFullName(),
                targetUser.getEmail()
        );
    }

    @DeleteMapping("{id}/")
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var mem = organizationalUnitMembershipService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        if (!user.getGlobalAdmin()) {
            var hasAccess = vOrganizationalUnitMembershipWithPermissionsRepository
                    .existsByUserIdAndOrganizationalUnitIdAndOrgUnitMemberPermissionEditIsTrue(
                            user.getId(),
                            mem.getOrganizationalUnitId()
                    );

            if (!hasAccess) {
                throw ResponseException
                        .forbidden("Sie benötigen die globale Rolle „Superadmin“ order „Systemadministrator:in“ order benötigen eine Organisationsrolle mit der Berechtigung „Organisationseinheit bearbeiten“.");
            }
        }

        var deletedMembership = organizationalUnitMembershipService
                .deleteEntity(mem);

        auditService.logAction(user, AuditAction.Delete, OrganizationalUnitMembershipEntity.class, Map.of(
                "id", deletedMembership.getId(),
                "orgUnitId", deletedMembership.getOrganizationalUnitId(),
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
