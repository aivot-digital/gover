package de.aivot.gover.backend.dataObject.controllers;

import de.aivot.gover.backend.audit.enums.AuditAction;
import de.aivot.gover.backend.audit.services.AuditService;
import de.aivot.gover.backend.audit.services.ScopedAuditService;
import de.aivot.gover.backend.dataObject.dtos.DataObjectItemRequestDTO;
import de.aivot.gover.backend.dataObject.dtos.DataObjectItemResponseDTO;
import de.aivot.gover.backend.dataObject.entities.DataObjectItemEntity;
import de.aivot.gover.backend.dataObject.entities.DataObjectItemEntityId;
import de.aivot.gover.backend.dataObject.filters.DataObjectItemFilter;
import de.aivot.gover.backend.dataObject.permissions.DataObjectPermissionProvider;
import de.aivot.gover.backend.dataObject.services.DataObjectItemService;
import de.aivot.gover.backend.dataObject.services.DataObjectSchemaService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.openApi.OpenApiConstants;
import de.aivot.gover.backend.permissions.services.PermissionService;
import de.aivot.gover.backend.user.services.UserService;
import de.aivot.gover.backend.utils.StringUtils;
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

import java.util.Map;

@RestController
@RequestMapping("/api/data-objects/{schemaKey}/items/")
@Tag(
        name = OpenApiConstants.Tags.DataObjectItemsName,
        description = OpenApiConstants.Tags.DataObjectItemsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class DataObjectItemController {
    private final ScopedAuditService auditService;
    private final DataObjectItemService service;
    private final DataObjectSchemaService schemaService;
    private final UserService userService;
    private final PermissionService permissionService;

    @Autowired
    public DataObjectItemController(AuditService auditService,
                                    DataObjectItemService service,
                                    DataObjectSchemaService schemaService,
                                    UserService userService,
                                    PermissionService permissionService) {
        this.auditService = auditService.createScopedAuditService(DataObjectItemController.class, "Datenobjekte");
        this.service = service;
        this.schemaService = schemaService;
        this.userService = userService;
        this.permissionService = permissionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Data Object Items",
            description = "Retrieve a paginated list of data object items for a specific schema with optional filtering. " +
                    "This requires the permission „" + DataObjectPermissionProvider.OBJECT_ITEM_READ + "“."
    )
    public Page<DataObjectItemResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid DataObjectItemFilter filter,
            @Nonnull @PathVariable String schemaKey
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, DataObjectPermissionProvider.OBJECT_ITEM_READ);

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
                    "This requires the permission „" + DataObjectPermissionProvider.OBJECT_ITEM_CREATE + "“."
    )
    public DataObjectItemResponseDTO create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody DataObjectItemRequestDTO requestDTO,
            @Nonnull @PathVariable String schemaKey
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .requireSystemPermission(execUser.getId(), DataObjectPermissionProvider.OBJECT_ITEM_CREATE);

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
                    "This requires the permission „" + DataObjectPermissionProvider.OBJECT_ITEM_READ + "“."
    )
    public DataObjectItemResponseDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String schemaKey,
            @Nonnull @PathVariable String itemId
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, DataObjectPermissionProvider.OBJECT_ITEM_READ);

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
                    "This requires the permission „" + DataObjectPermissionProvider.OBJECT_ITEM_UPDATE + "“."
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

        permissionService
                .requireSystemPermission(execUser.getId(), DataObjectPermissionProvider.OBJECT_ITEM_UPDATE);

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
                    "This requires the permission „" + DataObjectPermissionProvider.OBJECT_ITEM_DELETE + "“."
    )
    public void destroy(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String schemaKey,
            @Nonnull @PathVariable String itemId
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .requireSystemPermission(execUser.getId(), DataObjectPermissionProvider.OBJECT_ITEM_DELETE);

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
