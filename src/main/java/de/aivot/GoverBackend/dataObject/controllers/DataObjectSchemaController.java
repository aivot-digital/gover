package de.aivot.GoverBackend.dataObject.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.dataObject.entities.DataObjectItemEntity;
import de.aivot.GoverBackend.dataObject.entities.DataObjectSchemaEntity;
import de.aivot.GoverBackend.dataObject.filters.DataObjectSchemaFilter;
import de.aivot.GoverBackend.dataObject.services.DataObjectSchemaService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.user.services.UserService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/data-objects/")
@Tag(
        name = "Data Objects",
        description = "Data Objects are separated into schemas and items. " +
                      "Schemas define the structure of the data objects, while items are the actual data entries conforming to these schemas. " +
                      "Data Objects are used to store flexible and dynamic data within the application."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class DataObjectSchemaController {
    private final ScopedAuditService auditService;
    private final DataObjectSchemaService service;
    private final UserService userService;

    @Autowired
    public DataObjectSchemaController(AuditService auditService,
                                      DataObjectSchemaService service, UserService userService) {
        this.auditService = auditService.createScopedAuditService(DataObjectSchemaController.class, "Datenmodelle");

        this.service = service;
        this.userService = userService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Data Object Schemas",
            description = "Retrieve a paginated list of data object schemas. " +
                          "Supports filtering based on various criteria."
    )
    public Page<DataObjectSchemaEntity> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid DataObjectSchemaFilter filter
    ) throws ResponseException {
        return service
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Data Object Schema",
            description = "Create a new data object schema. " +
                          "Requires system administrator privileges."
    )
    public DataObjectSchemaEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody DataObjectSchemaEntity newDataObjectEntity
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSystemAdmin()
                .orElseThrow(ResponseException::noSystemAdminPermission);

        var created = service.create(newDataObjectEntity);

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.create().withUser(execUser).withAuditAction(AuditAction.Create, this.getClass().getSimpleName(), DataObjectItemEntity.class, "legacy", "legacy", Map.of(
                "schemaKey", created.getKey()
        )));

        return created;
    }

    @GetMapping("{key}/")
    @Operation(
            summary = "Retrieve Data Object Schema",
            description = "Retrieve a specific data object schema by its unique key."
    )
    public DataObjectSchemaEntity retrieve(
            @Nonnull @PathVariable String key
    ) throws ResponseException {
        return service
                .retrieve(key)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{key}/")
    @Operation(
            summary = "Update Data Object Schema",
            description = "Update an existing data object schema. " +
                          "Requires system administrator privileges."
    )
    public DataObjectSchemaEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String key,
            @Nonnull @Valid @RequestBody DataObjectSchemaEntity updatedDataObjectEntity
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSystemAdmin()
                .orElseThrow(ResponseException::noSystemAdminPermission);

        var updated = service
                .update(key, updatedDataObjectEntity);

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.create().withUser(execUser).withAuditAction(AuditAction.Update, this.getClass().getSimpleName(), DataObjectItemEntity.class, "legacy", "legacy", Map.of(
                "schemaKey", updated.getKey()
        )));

        return updated;
    }

    @DeleteMapping("{key}/")
    @Operation(
            summary = "Delete Data Object Schema",
            description = "Delete an existing data object schema. " +
                          "Requires system administrator privileges."
    )
    public void destroy(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String key
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSystemAdmin()
                .orElseThrow(ResponseException::noSystemAdminPermission);

        var deleted = service.delete(key);

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.create().withUser(execUser).withAuditAction(AuditAction.Delete, this.getClass().getSimpleName(), DataObjectItemEntity.class, "legacy", "legacy", Map.of(
                "schemaKey", deleted.getKey()
        )));
    }
}
