package de.aivot.GoverBackend.dataObject.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.models.AuditLogPayload;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.dataObject.dtos.DataObjectItemRequestDTO;
import de.aivot.GoverBackend.dataObject.dtos.DataObjectItemResponseDTO;
import de.aivot.GoverBackend.dataObject.entities.DataObjectItemEntity;
import de.aivot.GoverBackend.dataObject.entities.DataObjectItemEntityId;
import de.aivot.GoverBackend.dataObject.filters.DataObjectItemFilter;
import de.aivot.GoverBackend.dataObject.services.DataObjectItemService;
import de.aivot.GoverBackend.dataObject.services.DataObjectSchemaService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.utils.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/data-objects/{schemaKey}/items/")
@Tag(
        name = "Data Objects",
        description = "Data Objects are separated into schemas and items. " +
                "Schemas define the structure of the data objects, while items are the actual data entries conforming to these schemas. " +
                "Data Objects are used to store flexible and dynamic data within the application."
)
public class DataObjectItemController {
    private final ScopedAuditService auditService;
    private final DataObjectItemService service;
    private final DataObjectSchemaService schemaService;
    private final UserService userService;

    @Autowired
    public DataObjectItemController(AuditService auditService,
                                    DataObjectItemService service,
                                    DataObjectSchemaService schemaService, UserService userService) {
        this.auditService = auditService.createScopedAuditService(DataObjectItemController.class, "Datenobjekte");
        this.service = service;
        this.schemaService = schemaService;
        this.userService = userService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Data Object Items",
            description = "Retrieve a paginated list of data object items for a specific schema with optional filtering."
    )
    public Page<DataObjectItemResponseDTO> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid DataObjectItemFilter filter,
            @Nonnull @PathVariable String schemaKey
    ) throws ResponseException {
        filter.setSchemaKey(schemaKey);

        var schema = schemaService
                .retrieve(schemaKey)
                .orElseThrow(ResponseException::notFound);

        return service
                .list(pageable, filter)
                .map(i -> DataObjectItemResponseDTO.fromEntity(i, schema));
    }

    @PostMapping("")
    @Operation(
            summary = "Create Data Object Item",
            description = "Create a new data object item under a specific schema. " +
                    "Any user can create data object items."
    )
    public DataObjectItemResponseDTO create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody DataObjectItemRequestDTO requestDTO,
            @Nonnull @PathVariable String schemaKey
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var schema = schemaService
                .retrieve(schemaKey)
                .orElseThrow(ResponseException::notFound);

        var entity = requestDTO
                .toEntity(schema);

        var created = service
                .create(entity);

        auditService.create()
                .withUser(execUser)
                .withAuditAction(
                        AuditAction.Create,
                        DataObjectItemEntity.class,
                        entity.getId(),
                        "id",
                        Map.of(
                                "schemaKey", schemaKey
                        )
                )
                .withMessage(
                        "Ein neues Datenobjekt wurde von der Mitarbeiter:in %s erstellt.",
                        StringUtils.quote(execUser.getFullName())
                )
                .log();

        return DataObjectItemResponseDTO
                .fromEntity(created, schema);
    }

    @GetMapping("{itemId}/")
    @Operation(
            summary = "Retrieve Data Object Item",
            description = "Retrieve a specific data object item by its ID under a specific schema. " +
                    "Any user can retrieve data object items."
    )
    public DataObjectItemResponseDTO retrieve(
            @Nonnull @PathVariable String schemaKey,
            @Nonnull @PathVariable String itemId
    ) throws ResponseException {
        var schema = schemaService
                .retrieve(schemaKey)
                .orElseThrow(ResponseException::notFound);

        var id = new DataObjectItemEntityId(schemaKey, itemId);

        return service
                .retrieve(id)
                .filter(entity -> entity.getDeleted() == null)
                .map(i -> DataObjectItemResponseDTO.fromEntity(i, schema))
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{itemId}/")
    @Operation(
            summary = "Update Data Object Item",
            description = "Update an existing data object item under a specific schema. " +
                    "Any user can update data object items."
    )
    public DataObjectItemResponseDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String schemaKey,
            @Nonnull @PathVariable String itemId,
            @Nonnull @Valid @RequestBody DataObjectItemRequestDTO requestDTO
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var schema = schemaService
                .retrieve(schemaKey)
                .orElseThrow(ResponseException::notFound);

        var id = new DataObjectItemEntityId(schemaKey, itemId);

        var entity = requestDTO
                .toEntity(schema);

        var updated = service
                .update(id, entity);

        auditService.create()
                .withUser(execUser)
                .withAuditAction(
                        AuditAction.Update,
                        DataObjectItemEntity.class,
                        updated.getId(),
                        "id",
                        Map.of(
                                "schemaKey", schemaKey
                        )
                )
                .withMessage(
                        "Das Datenobjekt mit der ID %s wurde von der Mitarbeiter:in %s aktualisiert.",
                        StringUtils.quote(updated.getId()),
                        StringUtils.quote(execUser.getFullName())
                )
                .log(); // TODO: Add Diff

        return DataObjectItemResponseDTO
                .fromEntity(updated, schema);
    }

    @DeleteMapping("{itemId}/")
    @Operation(
            summary = "Delete Data Object Item",
            description = "Delete a specific data object item by its ID under a specific schema. " +
                    "Any user can delete data object items."
    )
    public void destroy(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String schemaKey,
            @Nonnull @PathVariable String itemId
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var id = new DataObjectItemEntityId(schemaKey, itemId);
        var deleted = service.delete(id);

        auditService.create()
                .withUser(execUser)
                .withAuditAction(
                        AuditAction.Delete,
                        DataObjectItemEntity.class,
                        deleted.getId(),
                        "id",
                        Map.of(
                                "schemaKey", schemaKey
                        )
                )
                .withMessage(
                        "Das Datenobjekt mit der ID %s wurde von der Mitarbeiter:in %s gelöscht.",
                        StringUtils.quote(deleted.getId()),
                        StringUtils.quote(execUser.getFullName())
                )
                .log();
    }
}
