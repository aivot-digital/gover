package de.aivot.GoverBackend.resourceAccessControl.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.resourceAccessControl.dtos.ResourceAccessControlRequestDTO;
import de.aivot.GoverBackend.resourceAccessControl.dtos.ResourceAccessControlResponseDTO;
import de.aivot.GoverBackend.resourceAccessControl.entities.ResourceAccessControlEntity;
import de.aivot.GoverBackend.resourceAccessControl.filters.ResourceAccessControlFilter;
import de.aivot.GoverBackend.resourceAccessControl.services.ResourceAccessControlService;
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
@RequestMapping("/api/resource-access-controls/")
public class ResourceAccessControlController {
    private final ScopedAuditService auditService;
    private final ResourceAccessControlService resourceAccessControlService;

    @Autowired
    public ResourceAccessControlController(AuditService auditService, ResourceAccessControlService resourceAccessControlService) {
        this.auditService = auditService
                .createScopedAuditService(ResourceAccessControlController.class);

        this.resourceAccessControlService = resourceAccessControlService;
    }

    @GetMapping("")
    public Page<ResourceAccessControlResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid ResourceAccessControlFilter filter
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return resourceAccessControlService
                .list(pageable, filter)
                .map(ResourceAccessControlResponseDTO::fromEntity);
    }

    @PostMapping("")
    public ResourceAccessControlResponseDTO create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid ResourceAccessControlRequestDTO requestDTO
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var created = resourceAccessControlService
                .create(requestDTO.toEntity());

        auditService
                .logAction(user, AuditAction.Create, ResourceAccessControlEntity.class, Map.of(
                        "id", created.getId()
                ));

        return ResourceAccessControlResponseDTO
                .fromEntity(created);
    }

    @GetMapping("{id}/")
    public ResourceAccessControlResponseDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return resourceAccessControlService
                .retrieve(id)
                .map(ResourceAccessControlResponseDTO::fromEntity)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    public ResourceAccessControlResponseDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @RequestBody @Valid ResourceAccessControlRequestDTO requestDTO
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var result = resourceAccessControlService
                .update(id, requestDTO.toEntity());

        auditService
                .logAction(user, AuditAction.Update, ResourceAccessControlEntity.class, Map.of(
                        "id", result.getId()
                ));

        return ResourceAccessControlResponseDTO
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

        var entity = resourceAccessControlService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        resourceAccessControlService
                .deleteEntity(entity);

        auditService
                .logAction(user, AuditAction.Delete, ResourceAccessControlEntity.class, Map.of(
                        "id", entity.getId()
                ));
    }
}

