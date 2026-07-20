package de.aivot.gover.backend.dataObject.controllers;

import de.aivot.gover.backend.audit.enums.AuditAction;
import de.aivot.gover.backend.audit.services.AuditService;
import de.aivot.gover.backend.audit.services.ScopedAuditService;
import de.aivot.gover.backend.dataObject.entities.DataObjectSchemaEntity;
import de.aivot.gover.backend.dataObject.filters.DataObjectSchemaFilter;
import de.aivot.gover.backend.dataObject.permissions.DataObjectPermissionProvider;
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

@RestController
@RequestMapping("/api/data-objects/")
@Tag(
        name = OpenApiConstants.Tags.DataObjectSchemasName,
        description = OpenApiConstants.Tags.DataObjectSchemasDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class DataObjectSchemaController {
    private final ScopedAuditService auditService;
    private final DataObjectSchemaService service;
    private final UserService userService;
    private final PermissionService permissionService;

    @Autowired
    public DataObjectSchemaController(AuditService auditService,
                                      DataObjectSchemaService service,
                                      UserService userService,
                                      PermissionService permissionService) {
        this.auditService = auditService.createScopedAuditService(DataObjectSchemaController.class, "Datenmodelle");

        this.service = service;
        this.userService = userService;
        this.permissionService = permissionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Data Object Schemas",
            description = "Retrieve a paginated list of data object schemas. " +
                    "Supports filtering based on various criteria. " +
                    "This requires the permission „" + DataObjectPermissionProvider.OBJECT_SCHEMA_READ + "“."
    )
    public Page<DataObjectSchemaEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid DataObjectSchemaFilter filter
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, DataObjectPermissionProvider.OBJECT_SCHEMA_READ);

        return service
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Data Object Schema",
            description = "Create a new data object schema. " +
                    "This requires the permission „" + DataObjectPermissionProvider.OBJECT_SCHEMA_CREATE + "“."
    )
    public DataObjectSchemaEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody DataObjectSchemaEntity newDataObjectEntity
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .requireSystemPermission(execUser.getId(), DataObjectPermissionProvider.OBJECT_SCHEMA_CREATE);

        var created = service.create(newDataObjectEntity);

        auditService.create()
                .withUser(execUser)
                .withAuditAction(
                        AuditAction.Create,
                        DataObjectSchemaEntity.class,
                        created.getKey(),
                        "key"
                )
                .withMessage(
                        "Das Datenmodell mit dem Schlüssel %s wurde von der Mitarbeiter:in %s erstellt.",
                        StringUtils.quote(created.getKey()),
                        StringUtils.quote(execUser.getFullName())
                )
                .log();

        return created;
    }

    @GetMapping("{key}/")
    @Operation(
            summary = "Retrieve Data Object Schema",
            description = "Retrieve a specific data object schema by its unique key. " +
                    "This requires the permission „" + DataObjectPermissionProvider.OBJECT_SCHEMA_READ + "“."
    )
    public DataObjectSchemaEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String key
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, DataObjectPermissionProvider.OBJECT_SCHEMA_READ);

        return service
                .retrieve(key)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{key}/")
    @Operation(
            summary = "Update Data Object Schema",
            description = "Update an existing data object schema. " +
                    "This requires the permission „" + DataObjectPermissionProvider.OBJECT_SCHEMA_UPDATE + "“."
    )
    public DataObjectSchemaEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String key,
            @Nonnull @Valid @RequestBody DataObjectSchemaEntity updatedDataObjectEntity
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .requireSystemPermission(execUser.getId(), DataObjectPermissionProvider.OBJECT_SCHEMA_UPDATE);

        var updated = service
                .update(key, updatedDataObjectEntity);

        auditService.create()
                .withUser(execUser)
                .withAuditAction(
                        AuditAction.Update,
                        DataObjectSchemaEntity.class,
                        updated.getKey(), "key"
                )
                .withMessage(
                        "Das Datenmodell mit dem Schlüssel %s wurde von der Mitarbeiter:in %s aktualisiert.",
                        StringUtils.quote(updated.getKey()),
                        StringUtils.quote(execUser.getFullName())
                )
                .log(); // TODO: Add Diff

        return updated;
    }

    @DeleteMapping("{key}/")
    @Operation(
            summary = "Delete Data Object Schema",
            description = "Delete an existing data object schema. " +
                    "This requires the permission „" + DataObjectPermissionProvider.OBJECT_SCHEMA_DELETE + "“."
    )
    public void destroy(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String key
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .requireSystemPermission(execUser.getId(), DataObjectPermissionProvider.OBJECT_SCHEMA_DELETE);

        var deleted = service.delete(key);

        auditService.create()
                .withUser(execUser)
                .withAuditAction(
                        AuditAction.Delete,
                        DataObjectSchemaEntity.class,
                        deleted.getKey(),
                        "key"
                )
                .withMessage(
                        "Das Datenmodell mit dem Schlüssel %s wurde von der Mitarbeiter:in %s gelöscht.",
                        StringUtils.quote(deleted.getKey()),
                        StringUtils.quote(execUser.getFullName())
                )
                .log();
    }
}
