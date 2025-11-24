package de.aivot.GoverBackend.userRoles.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.payment.controllers.staff.PaymentProviderController;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.userRoles.dtos.UserRoleRequestDTO;
import de.aivot.GoverBackend.userRoles.dtos.UserRoleResponseDTO;
import de.aivot.GoverBackend.userRoles.entities.UserRoleEntity;
import de.aivot.GoverBackend.userRoles.filters.UserRoleFilter;
import de.aivot.GoverBackend.userRoles.services.UserRoleService;
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
@RequestMapping("/api/user-roles/")
public class UserRoleController {
    private final ScopedAuditService auditService;
    private final UserRoleService userRoleService;

    @Autowired
    public UserRoleController(AuditService auditService, UserRoleService userRoleService) {
        this.auditService = auditService
                .createScopedAuditService(UserRoleController.class);

        this.userRoleService = userRoleService;
    }

    @GetMapping("")
    public Page<UserRoleResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid UserRoleFilter filter
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return userRoleService
                .list(pageable, filter)
                .map(UserRoleResponseDTO::fromEntity);
    }

    @PostMapping("")
    public UserRoleResponseDTO create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid UserRoleRequestDTO requestDTO
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var created = userRoleService
                .create(requestDTO.toEntity());

        auditService
                .logAction(user, AuditAction.Create, UserRoleEntity.class, Map.of(
                        "id", created.getId(),
                        "name", created.getName()
                ));

        return UserRoleResponseDTO
                .fromEntity(created);
    }

    @GetMapping("{id}/")
    public UserRoleResponseDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return userRoleService
                .retrieve(id)
                .map(UserRoleResponseDTO::fromEntity)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    public UserRoleResponseDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @RequestBody @Valid UserRoleRequestDTO requestDTO
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var result = userRoleService
                .update(id, requestDTO.toEntity());

        auditService
                .logAction(user, AuditAction.Update, UserRoleEntity.class, Map.of(
                        "id", result.getId(),
                        "name", result.getName()
                ));

        return UserRoleResponseDTO
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

        var entity = userRoleService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        userRoleService
                .deleteEntity(entity);

        auditService
                .logAction(user, AuditAction.Delete, UserRoleEntity.class, Map.of(
                        "id", entity.getId(),
                        "name", entity.getName()
                ));
    }
}
