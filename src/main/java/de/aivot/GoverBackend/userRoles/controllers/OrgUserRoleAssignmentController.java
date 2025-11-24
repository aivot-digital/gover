package de.aivot.GoverBackend.userRoles.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.department.services.VOrganizationalUnitMembershipWithDetailsService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.payment.controllers.staff.PaymentProviderController;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.userRoles.dtos.UserRoleAssignmentRequestDTO;
import de.aivot.GoverBackend.userRoles.dtos.OrgUserRoleAssignmentResponseDTO;
import de.aivot.GoverBackend.userRoles.entities.UserRoleAssignmentEntity;
import de.aivot.GoverBackend.userRoles.filters.VOrgUserRoleAssignmentsWithDetailsFilter;
import de.aivot.GoverBackend.userRoles.services.UserRoleAssignmentService;
import de.aivot.GoverBackend.userRoles.services.UserRoleService;
import de.aivot.GoverBackend.userRoles.services.VOrgUserRoleAssignmentsWithDetailsService;
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
@RequestMapping("/api/org-user-role-assignments/")
public class OrgUserRoleAssignmentController {
    private final ScopedAuditService auditService;
    private final UserRoleAssignmentService userRoleAssignmentService;
    private final VOrgUserRoleAssignmentsWithDetailsService vOrgUserRoleAssignmentsWithDetailsService;
    private final UserRoleService userRoleService;
    private final VOrganizationalUnitMembershipWithDetailsService vOrganizationalUnitMembershipWithDetailsService;

    @Autowired
    public OrgUserRoleAssignmentController(AuditService auditService,
                                           UserRoleAssignmentService userRoleAssignmentService,
                                           VOrgUserRoleAssignmentsWithDetailsService vOrgUserRoleAssignmentsWithDetailsService, UserRoleService userRoleService, VOrganizationalUnitMembershipWithDetailsService vOrganizationalUnitMembershipWithDetailsService) {
        this.auditService = auditService.createScopedAuditService(PaymentProviderController.class);
        this.userRoleAssignmentService = userRoleAssignmentService;
        this.vOrgUserRoleAssignmentsWithDetailsService = vOrgUserRoleAssignmentsWithDetailsService;
        this.userRoleService = userRoleService;
        this.vOrganizationalUnitMembershipWithDetailsService = vOrganizationalUnitMembershipWithDetailsService;
    }

    @GetMapping("")
    public Page<OrgUserRoleAssignmentResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid VOrgUserRoleAssignmentsWithDetailsFilter filter
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return vOrgUserRoleAssignmentsWithDetailsService
                .list(pageable, filter)
                .map(OrgUserRoleAssignmentResponseDTO::fromEntity);
    }

    @PostMapping("")
    public OrgUserRoleAssignmentResponseDTO create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid UserRoleAssignmentRequestDTO requestDTO
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var created = userRoleAssignmentService
                .create(requestDTO.toEntity());

        auditService
                .logAction(user, AuditAction.Create, UserRoleAssignmentEntity.class, Map.of(
                        "id", created.getId(),
                        "userRoleId", created.getUserRoleId()
                ));

        var userRole = userRoleService
                .retrieve(created.getUserRoleId())
                .orElseThrow(() -> ResponseException.internalServerError("User role not found for created assignment"));

        var mem = vOrganizationalUnitMembershipWithDetailsService
                .retrieve(created.getOrganizationalUnitMembershipId())
                .orElseThrow(() -> ResponseException.internalServerError("Organizational unit membership not found for created assignment"));

        return OrgUserRoleAssignmentResponseDTO
                .fromEntity(created, userRole, mem);
    }

    @GetMapping("{id}/")
    public OrgUserRoleAssignmentResponseDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return vOrgUserRoleAssignmentsWithDetailsService
                .retrieve(id)
                .map(OrgUserRoleAssignmentResponseDTO::fromEntity)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    public OrgUserRoleAssignmentResponseDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @RequestBody @Valid UserRoleAssignmentRequestDTO requestDTO
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var result = userRoleAssignmentService
                .update(id, requestDTO.toEntity());

        auditService
                .logAction(user, AuditAction.Update, UserRoleAssignmentEntity.class, Map.of(
                        "id", result.getId(),
                        "userRoleId", result.getUserRoleId()
                ));

        var userRole = userRoleService
                .retrieve(result.getUserRoleId())
                .orElseThrow(() -> ResponseException.internalServerError("User role not found for updated assignment"));

        var mem = vOrganizationalUnitMembershipWithDetailsService
                .retrieve(result.getOrganizationalUnitMembershipId())
                .orElseThrow(() -> ResponseException.internalServerError("Organizational unit membership not found for updated assignment"));

        return OrgUserRoleAssignmentResponseDTO
                .fromEntity(result, userRole, mem);
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

        userRoleAssignmentService
                .deleteEntity(entity);

        auditService
                .logAction(user, AuditAction.Delete, UserRoleAssignmentEntity.class, Map.of(
                        "id", entity.getId(),
                        "userRoleId", entity.getUserRoleId()
                ));
    }
}
