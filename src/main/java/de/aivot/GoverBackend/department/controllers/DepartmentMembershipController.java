package de.aivot.GoverBackend.department.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.department.dtos.DepartmentMembershipRequestDTO;
import de.aivot.GoverBackend.department.dtos.DepartmentMembershipResponseDTO;
import de.aivot.GoverBackend.department.entities.DepartmentMembershipEntity;
import de.aivot.GoverBackend.department.filters.DepartmentMembershipFilter;
import de.aivot.GoverBackend.department.filters.DepartmentWithMembershipFilter;
import de.aivot.GoverBackend.department.services.DepartmentMembershipService;
import de.aivot.GoverBackend.department.services.DepartmentService;
import de.aivot.GoverBackend.department.services.DepartmentWithMembershipService;
import de.aivot.GoverBackend.enums.UserRole;
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

    private final DepartmentWithMembershipService departmentWithMembershipService;
    private final DepartmentMembershipService departmentMembershipService;
    private final DepartmentMembershipMailService departmentMembershipMailService;
    private final ExceptionMailService exceptionMailService;
    private final UserService userService;
    private final DepartmentService departmentService;

    @Autowired
    public DepartmentMembershipController(
            AuditService auditService,
            DepartmentWithMembershipService departmentWithMembershipService,
            DepartmentMembershipService departmentMembershipService,
            DepartmentMembershipMailService departmentMembershipMailService,
            ExceptionMailService exceptionMailService,
            UserService userService,
            DepartmentService departmentService
    ) {
        this.auditService = auditService.createScopedAuditService(DepartmentMembershipController.class);

        this.departmentWithMembershipService = departmentWithMembershipService;
        this.departmentMembershipService = departmentMembershipService;
        this.departmentMembershipMailService = departmentMembershipMailService;
        this.exceptionMailService = exceptionMailService;
        this.userService = userService;
        this.departmentService = departmentService;
    }

    @GetMapping("")
    public Page<DepartmentMembershipResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid DepartmentWithMembershipFilter filter

    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return departmentWithMembershipService
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
            var spec = DepartmentMembershipFilter
                    .create()
                    .setDepartmentId(requestDTO.departmentId())
                    .setUserId(user.getId())
                    .setRole(UserRole.Admin)
                    .build();

            if (!departmentMembershipService.exists(spec)) {
                throw ResponseException.forbidden("Nur globale Administrator:innen oder Fachbereichsadministrator:innen können Fachbereichsmitglieder hinzufügen.");
            }
        }

        var targetUser = userService
                .retrieve(requestDTO.userId())
                .orElseThrow(ResponseException::badRequest);

        var department = departmentService
                .retrieve(requestDTO.departmentId())
                .orElseThrow(ResponseException::badRequest);

        var departmentMembership = departmentMembershipService
                .create(requestDTO.toEntity());

        auditService.logAction(user, AuditAction.Create, DepartmentMembershipEntity.class, Map.of(
                "id", departmentMembership.getId(),
                "departmentId", departmentMembership.getDepartmentId(),
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
                departmentMembership.getDepartmentId(),
                department.getName(),
                departmentMembership.getUserId(),
                departmentMembership.getRole(),
                targetUser.getFirstName(),
                targetUser.getLastName(),
                targetUser.getFullName(),
                targetUser.getEmail(),
                targetUser.getEnabled(),
                targetUser.getVerified(),
                targetUser.getDeletedInIdp(),
                targetUser.getGlobalAdmin()
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

        var mem = departmentMembershipService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        var department = departmentService
                .retrieve(mem.getDepartmentId())
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
            var spec = DepartmentMembershipFilter
                    .create()
                    .setDepartmentId(requestDTO.departmentId())
                    .setUserId(user.getId())
                    .setRole(UserRole.Admin)
                    .build();

            if (!departmentMembershipService.exists(spec)) {
                throw ResponseException.forbidden("Nur globale Administrator:innen oder Fachbereichsadministrator:innen können Fachbereichsmitglieder bearbeiten.");
            }
        }

        var existingMembership = departmentMembershipService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        UserRole oldRole = existingMembership.getRole();
        UserRole newRole = requestDTO.role();

        var targetUser = userService
                .retrieve(requestDTO.userId())
                .orElseThrow(ResponseException::notFound);

        var department = departmentService
                .retrieve(requestDTO.departmentId())
                .orElseThrow(ResponseException::notFound);

        var updatedMembership = departmentMembershipService
                .update(id, requestDTO.toEntity());

        if (!oldRole.equals(newRole)) {
            try {
                departmentMembershipMailService.sendRoleChanged(user, updatedMembership, oldRole, newRole);
            } catch (MessagingException | IOException | InvalidUserEMailException e) {
                exceptionMailService.send(e);
            }
        }

        return new DepartmentMembershipResponseDTO(
                updatedMembership.getId(),
                updatedMembership.getDepartmentId(),
                department.getName(),
                updatedMembership.getUserId(),
                updatedMembership.getRole(),
                targetUser.getFirstName(),
                targetUser.getLastName(),
                targetUser.getFullName(),
                targetUser.getEmail(),
                targetUser.getEnabled(),
                targetUser.getVerified(),
                targetUser.getDeletedInIdp(),
                targetUser.getGlobalAdmin()
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

        if (!user.getGlobalAdmin()) {
            var membershipToDelete = departmentMembershipService
                    .retrieve(id)
                    .orElseThrow(ResponseException::notFound);

            var spec = DepartmentMembershipFilter
                    .create()
                    .setDepartmentId(membershipToDelete.getDepartmentId())
                    .setUserId(user.getId())
                    .setRole(UserRole.Admin)
                    .build();

            if (!departmentMembershipService.exists(spec)) {
                throw ResponseException.forbidden("Nur globale Administrator:innen oder Fachbereichsadministrator:innen können Fachbereichsmitglieder löschen.");
            }
        }

        var deletedMembership = departmentMembershipService.delete(id);

        auditService.logAction(user, AuditAction.Delete, DepartmentMembershipEntity.class, Map.of(
                "id", deletedMembership.getId(),
                "departmentId", deletedMembership.getDepartmentId(),
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
