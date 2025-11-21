package de.aivot.GoverBackend.userRoles.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.payment.controllers.staff.PaymentProviderController;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.userRoles.dtos.UserRoleAssignmentRequestDTO;
import de.aivot.GoverBackend.userRoles.dtos.UserRoleAssignmentResponseDTO;
import de.aivot.GoverBackend.userRoles.entities.UserRoleAssignmentEntity;
import de.aivot.GoverBackend.userRoles.filters.UserRoleAssignmentFilter;
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

@RestController
@RequestMapping("/api/user-role-assignments/")
public class UserRoleAssignmentController {
    private final ScopedAuditService auditService;
    private final UserRoleAssignmentService userRoleAssignmentService;

    @Autowired
    public UserRoleAssignmentController(AuditService auditService, UserRoleAssignmentService userRoleAssignmentService) {
        this.auditService = auditService.createScopedAuditService(PaymentProviderController.class);
        this.userRoleAssignmentService = userRoleAssignmentService;
    }

    @GetMapping("")
    public Page<UserRoleAssignmentResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid UserRoleAssignmentFilter filter
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return userRoleAssignmentService
                .list(pageable, filter)
                .map(UserRoleAssignmentResponseDTO::fromEntity);
    }

    @PostMapping("")
    public UserRoleAssignmentResponseDTO create(
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

        return UserRoleAssignmentResponseDTO
                .fromEntity(created);
    }

    @GetMapping("{id}/")
    public UserRoleAssignmentResponseDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return userRoleAssignmentService
                .retrieve(id)
                .map(UserRoleAssignmentResponseDTO::fromEntity)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    public UserRoleAssignmentResponseDTO update(
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

        return UserRoleAssignmentResponseDTO
                .fromEntity(result);
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
