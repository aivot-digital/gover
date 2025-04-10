package de.aivot.GoverBackend.destination.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.destination.dtos.DestinationRequestDTO;
import de.aivot.GoverBackend.destination.dtos.DestinationResponseDTO;
import de.aivot.GoverBackend.destination.entities.Destination;
import de.aivot.GoverBackend.destination.filters.DestinationFilter;
import de.aivot.GoverBackend.destination.services.DestinationService;
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
@RequestMapping("/api/destinations/")
public class DestinationController {
    private final ScopedAuditService auditService;

    private final DestinationService destinationService;

    @Autowired
    public DestinationController(
            AuditService auditService,
            DestinationService destinationService
    ) {
        this.auditService = auditService
                .createScopedAuditService(DestinationController.class);

        this.destinationService = destinationService;
    }

    @GetMapping("")
    public Page<DestinationResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid DestinationFilter filter
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return destinationService
                .list(pageable, filter)
                .map(DestinationResponseDTO::fromEntity);
    }

    @PostMapping("")
    public DestinationResponseDTO create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody DestinationRequestDTO requestDTO
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var entity = destinationService
                .create(requestDTO.toEntity());

        auditService
                .logAction(
                        user,
                        AuditAction.Create,
                        Destination.class,
                        Map.of(
                                "id", entity.getId(),
                                "name", entity.getName()
                        )
                );

        return DestinationResponseDTO
                .fromEntity(entity);
    }

    @GetMapping("{id}/")
    public DestinationResponseDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return destinationService
                .retrieve(id)
                .map(DestinationResponseDTO::fromEntity)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    public DestinationResponseDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @Valid @RequestBody DestinationRequestDTO requestDTO
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var entity = destinationService
                .update(id, requestDTO.toEntity());

        auditService
                .logAction(
                        user,
                        AuditAction.Update,
                        Destination.class,
                        Map.of(
                                "id", entity.getId(),
                                "name", entity.getName()
                        )
                );

        return DestinationResponseDTO
                .fromEntity(entity);
    }

    @DeleteMapping("{id}/")
    public void destroy(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var entity = destinationService
                .delete(id);

        auditService
                .logAction(
                        user,
                        AuditAction.Delete,
                        Destination.class,
                        Map.of(
                                "id", entity.getId(),
                                "name", entity.getName()
                        )
                );
    }
}
