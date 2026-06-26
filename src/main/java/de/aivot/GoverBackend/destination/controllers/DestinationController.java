package de.aivot.GoverBackend.destination.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.models.AuditLogPayload;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.destination.dtos.DestinationRequestDTO;
import de.aivot.GoverBackend.destination.dtos.DestinationResponseDTO;
import de.aivot.GoverBackend.destination.entities.Destination;
import de.aivot.GoverBackend.destination.filters.DestinationFilter;
import de.aivot.GoverBackend.destination.services.DestinationService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.utils.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;

@RestController
@RequestMapping("/api/destinations/")
@Tag(
        name = OpenApiConstants.Tags.DestinationsName,
        description = OpenApiConstants.Tags.DestinationsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class DestinationController {
    private final ScopedAuditService auditService;

    private final DestinationService destinationService;
    private final UserService userService;

    @Autowired
    public DestinationController(AuditService auditService,
                                 DestinationService destinationService,
                                 UserService userService) {
        this.auditService = auditService
                .createScopedAuditService(DestinationController.class, "Schnittstellen");

        this.destinationService = destinationService;
        this.userService = userService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Destinations",
            description = "Retrieve a paginated list of destinations. Supports filtering based on various criteria to narrow down the results."
    )
    public Page<DestinationResponseDTO> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid DestinationFilter filter
    ) throws ResponseException {
        return destinationService
                .list(pageable, filter)
                .map(DestinationResponseDTO::fromEntity);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Destination",
            description = "Create a new destination with the provided details. " +
                    "Requires system administrator permissions."
    )
    public DestinationResponseDTO create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody DestinationRequestDTO requestDTO
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSystemAdmin()
                .orElseThrow(ResponseException::noSystemAdminPermission);

        var entity = destinationService
                .create(requestDTO.toEntity());

        auditService.create()
                .withUser(execUser)
                .withAuditAction(
                        AuditAction.Create,
                        Destination.class,
                        entity.getId(),
                        "id",
                        Map.of(
                                "name", entity.getName()
                        ))
                .withMessage(
                        "Die Schnittstelle %s mit der ID %s wurde von der Mitarbeiter:in %s erstellt.",
                        StringUtils.quote(entity.getName()),
                        StringUtils.quote(String.valueOf(entity.getId())),
                        StringUtils.quote(execUser.getFullName())
                )
                .log();

        return DestinationResponseDTO
                .fromEntity(entity);
    }

    @GetMapping("{id}/")
    public DestinationResponseDTO retrieve(
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        return destinationService
                .retrieve(id)
                .map(DestinationResponseDTO::fromEntity)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update Destination",
            description = "Update an existing destination identified by its ID. " +
                    "Requires system administrator permissions."
    )
    public DestinationResponseDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @Valid @RequestBody DestinationRequestDTO requestDTO
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSystemAdmin()
                .orElseThrow(ResponseException::noSystemAdminPermission);

        var entity = destinationService
                .update(id, requestDTO.toEntity());

        auditService.create()
                .withUser(execUser)
                .withAuditAction(
                        AuditAction.Update,
                        Destination.class,
                        entity.getId(),
                        "id",
                        Map.of(
                                "name", entity.getName()
                        ))
                .withMessage(
                        "Die Schnittstelle %s mit der ID %s wurde von der Mitarbeiter:in %s aktualisiert.",
                        StringUtils.quote(entity.getName()),
                        StringUtils.quote(String.valueOf(entity.getId())),
                        StringUtils.quote(execUser.getFullName())
                )
                .log(); // TODO: Create Diff

        return DestinationResponseDTO
                .fromEntity(entity);
    }

    @DeleteMapping("{id}/")
    @Operation(
            summary = "Delete Destination",
            description = "Delete an existing destination identified by its ID. " +
                    "Requires system administrator permissions."
    )
    public void destroy(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSystemAdmin()
                .orElseThrow(ResponseException::noSystemAdminPermission);

        var entity = destinationService
                .delete(id);

        auditService.create()
                .withUser(user)
                .withAuditAction(
                        AuditAction.Delete,
                        Destination.class,
                        entity.getId(),
                        "id",
                        Map.of(
                                "name", entity.getName()
                        ))
                .withMessage(
                        "Die Schnittstelle %s mit der ID %s wurde von der Mitarbeiter:in %s gelöscht.",
                        StringUtils.quote(entity.getName()),
                        StringUtils.quote(String.valueOf(entity.getId())),
                        StringUtils.quote(user.getFullName())
                )
                .log();
    }
}
