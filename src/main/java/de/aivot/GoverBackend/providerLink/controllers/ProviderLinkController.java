package de.aivot.GoverBackend.providerLink.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.providerLink.dtos.ProviderLinkRequestDTO;
import de.aivot.GoverBackend.providerLink.dtos.ProviderLinkResponseDTO;
import de.aivot.GoverBackend.providerLink.entities.ProviderLink;
import de.aivot.GoverBackend.providerLink.filters.ProviderLinkFilter;
import de.aivot.GoverBackend.providerLink.services.ProviderLinkService;
import de.aivot.GoverBackend.user.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

@RestController
@RequestMapping("/api/provider-links/")
public class ProviderLinkController {
    private final ScopedAuditService auditService;
    private final ProviderLinkService providerLinkService;

    @Autowired
    public ProviderLinkController(
            AuditService auditService,
            ProviderLinkService providerLinkService
    ) {
        this.auditService = auditService.createScopedAuditService(ProviderLinkController.class);
        this.providerLinkService = providerLinkService;
    }

    @GetMapping("")
    public Page<ProviderLinkResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid ProviderLinkFilter filter
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return providerLinkService
                .list(pageable, filter)
                .map(ProviderLinkResponseDTO::fromEntity);
    }

    @PostMapping("")
    public ProviderLinkResponseDTO create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody ProviderLinkRequestDTO requestDTO
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var entity = providerLinkService
                .create(requestDTO.toEntity());

        auditService
                .logAction(
                        user,
                        AuditAction.Create,
                        ProviderLink.class,
                        Map.of(
                                "id", entity.getId(),
                                "text", entity.getText(),
                                "link", entity.getLink()
                        )
                );

        return ProviderLinkResponseDTO
                .fromEntity(entity);
    }

    @GetMapping("{id}/")
    public ProviderLinkResponseDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return providerLinkService
                .retrieve(id)
                .map(ProviderLinkResponseDTO::fromEntity)
                .orElseThrow(ResourceNotFoundException::new);
    }

    @PutMapping("{id}/")
    public ProviderLinkResponseDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @Valid @RequestBody ProviderLinkRequestDTO requestDTO
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var entity = providerLinkService
                .update(id, requestDTO.toEntity());

        auditService.logAction(
                user,
                AuditAction.Update,
                ProviderLink.class,
                Map.of(
                        "id", entity.getId(),
                        "text", entity.getText(),
                        "link", entity.getLink()
                )
        );

        return ProviderLinkResponseDTO
                .fromEntity(entity);
    }

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

        var link = providerLinkService
                .delete(id);

        auditService.logAction(
                user,
                AuditAction.Delete,
                ProviderLink.class,
                Map.of(
                        "id", link.getId(),
                        "text", link.getText(),
                        "link", link.getLink()
                )
        );
    }
}
