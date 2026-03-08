package de.aivot.GoverBackend.storage.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.permissions.services.PermissionService;
import de.aivot.GoverBackend.storage.entities.StorageIndexItemEntity;
import de.aivot.GoverBackend.storage.entities.StorageProviderEntity;
import de.aivot.GoverBackend.storage.enums.StorageProviderStatus;
import de.aivot.GoverBackend.storage.exceptions.StorageException;
import de.aivot.GoverBackend.storage.filters.StorageProviderFilter;
import de.aivot.GoverBackend.storage.permissions.StoragePermissionProvider;
import de.aivot.GoverBackend.storage.repositories.StorageIndexItemRepository;
import de.aivot.GoverBackend.storage.services.StorageProviderDefinitionService;
import de.aivot.GoverBackend.storage.services.StorageProviderService;
import de.aivot.GoverBackend.storage.services.StorageSyncWorker;
import de.aivot.GoverBackend.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/storage-providers/")
@Tag(
        name = OpenApiConstants.Tags.StorageProvidersName,
        description = OpenApiConstants.Tags.StorageProvidersDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class StorageProviderController {
    private final ScopedAuditService auditService;

    private final UserService userService;
    private final StorageProviderService storageProviderService;
    private final PermissionService permissionService;
    private final RabbitTemplate rabbitTemplate;
    private final StorageIndexItemRepository storageIndexItemRepository;
    private final StorageProviderDefinitionService storageProviderDefinitionService;

    @Autowired
    public StorageProviderController(AuditService auditService,
                                     UserService userService,
                                     StorageProviderService storageProviderService,
                                     PermissionService permissionService,
                                     RabbitTemplate rabbitTemplate,
                                     StorageIndexItemRepository storageIndexItemRepository,
                                     StorageProviderDefinitionService storageProviderDefinitionService) {
        this.auditService = auditService.createScopedAuditService(StorageProviderController.class);
        this.userService = userService;
        this.storageProviderService = storageProviderService;
        this.permissionService = permissionService;
        this.rabbitTemplate = rabbitTemplate;
        this.storageIndexItemRepository = storageIndexItemRepository;
        this.storageProviderDefinitionService = storageProviderDefinitionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Storage Providers",
            description = "Retrieve a paginated list of storage providers with optional filtering. Requires the permission " + StoragePermissionProvider.STORAGE_PROVIDER_READ + "."
    )
    public Page<StorageProviderEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid StorageProviderFilter filter
    ) throws ResponseException {
        permissionService
                .testSystemPermission(jwt, StoragePermissionProvider.STORAGE_PROVIDER_READ);

        return storageProviderService
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Storage Provider",
            description = "Create a new storage provider. Requires the permission " + StoragePermissionProvider.STORAGE_PROVIDER_CREATE + "."
    )
    public StorageProviderEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid StorageProviderEntity newStorageProvider
    ) throws ResponseException {
        permissionService
                .testSystemPermission(jwt, StoragePermissionProvider.STORAGE_PROVIDER_CREATE);

        var execUser = userService.fromJWTOrThrow(jwt);

        var created = storageProviderService
                .create(newStorageProvider);

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.create().withUser(execUser).withAuditAction(AuditAction.Create, this.getClass().getSimpleName(), StorageProviderEntity.class, "legacy", "legacy", Map.of(
                        "id", created.getId(),
                        "name", created.getName()
                )));

        return created;
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Storage Provider",
            description = "Retrieve details of a specific storage provider by its id. Requires the permission " + StoragePermissionProvider.STORAGE_PROVIDER_READ + "."
    )
    public StorageProviderEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        permissionService
                .testSystemPermission(jwt, StoragePermissionProvider.STORAGE_PROVIDER_READ);

        return storageProviderService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update Storage Provider",
            description = "Update an existing storage provider. Requires the permission " + StoragePermissionProvider.STORAGE_PROVIDER_UPDATE + "."
    )
    public StorageProviderEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @RequestBody @Valid StorageProviderEntity update
    ) throws ResponseException {
        permissionService
                .testSystemPermission(jwt, StoragePermissionProvider.STORAGE_PROVIDER_UPDATE);

        var execUser = userService.fromJWTOrThrow(jwt);

        var result = storageProviderService
                .update(id, update);

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.create().withUser(execUser).withAuditAction(AuditAction.Update, this.getClass().getSimpleName(), StorageProviderEntity.class, "legacy", "legacy", Map.of(
                        "id", result.getId(),
                        "name", result.getName()
                )));

        return result;
    }

    @DeleteMapping("{id}/")
    @Operation(
            summary = "Delete Storage Provider",
            description = "Delete an existing storage provider. Requires the permission " + StoragePermissionProvider.STORAGE_PROVIDER_DELETE + "."
    )
    public void destroy(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id
    ) throws ResponseException {
        permissionService
                .testSystemPermission(jwt, StoragePermissionProvider.STORAGE_PROVIDER_DELETE);

        var execUser = userService.fromJWTOrThrow(jwt);

        var deleted = storageProviderService
                .delete(id);

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.create().withUser(execUser).withAuditAction(AuditAction.Delete, this.getClass().getSimpleName(), StorageProviderEntity.class, "legacy", "legacy", Map.of(
                        "id", deleted.getId(),
                        "name", deleted.getName()
                )));
    }

    @PutMapping("{id}/resync/")
    @Operation(
            summary = "Resync Storage Provider",
            description = "Mark a storage provider for resynchronization. Requires the permission " + StoragePermissionProvider.STORAGE_PROVIDER_UPDATE + "."
    )
    public StorageProviderEntity markForResync(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id
    ) throws ResponseException {
        permissionService
                .testSystemPermission(jwt, StoragePermissionProvider.STORAGE_PROVIDER_UPDATE);

        var execUser = userService.fromJWTOrThrow(jwt);

        var storageProvider = storageProviderService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        var updated = storageProviderService
                .update(id, storageProvider.setStatus(StorageProviderStatus.SyncPending));

        // Queue the storage sync worker to do the resync
        rabbitTemplate
                .convertAndSend(StorageSyncWorker.DO_WORK_ON_STORAGE_SYNC_QUEUE, id);

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.create().withUser(execUser).withAuditAction(AuditAction.Update, this.getClass().getSimpleName(), StorageProviderEntity.class, "legacy", "legacy", Map.of(
                        "id", updated.getId(),
                        "name", updated.getName(),
                        "resynced", true
                )));

        return updated;
    }

    @GetMapping(value = {
            "{id}/folders/",
            "{id}/folders/**",
    })
    @Operation(
            summary = "Get Folder from Storage Provider",
            description = "Retrieve a folder from the specified storage provider. Requires the permission " + StoragePermissionProvider.STORAGE_PROVIDER_READ + "."
    )
    public List<StorageIndexItemEntity> getFolder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id,
            HttpServletRequest request
    ) throws ResponseException {
        permissionService
                .testSystemPermission(jwt, StoragePermissionProvider.STORAGE_PROVIDER_READ);

        var normalizedPath = getNormalizedPath(request, true);

        return storageIndexItemRepository
                .listAllInFolder(id, "^" + normalizedPath + "([^/]+$|[^/]+/$)", false);
    }

    @Nonnull
    private static String getNormalizedPath(HttpServletRequest request, boolean isDirectory) {
        var pathParts = request
                .getRequestURL()
                .toString()
                .split(isDirectory ? "/folders/" : "/files/");

        String normalizedPath = "/";
        if (pathParts.length > 1) {
            normalizedPath = pathParts[1];

            if (!normalizedPath.startsWith("/")) {
                normalizedPath = "/" + normalizedPath;
            }

            if (isDirectory && !normalizedPath.endsWith("/")) {
                normalizedPath = normalizedPath + "/";
            }
        }

        return URLDecoder.decode(normalizedPath, StandardCharsets.UTF_8);
    }

    @PostMapping("{id}/test/")
    @Operation(
            summary = "Test Storage Provider",
            description = "Manually test the connection to a storage provider. Uses the same logic as the health check. Optionally checks for writability."
    )
    public Map<String, Object> testStorageProvider(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id,
            @RequestParam(name = "writable", required = false, defaultValue = "false") boolean writable
    ) throws ResponseException {
        permissionService.testSystemPermission(jwt, StoragePermissionProvider.STORAGE_PROVIDER_READ);

        var provider = storageProviderService.retrieve(id).orElseThrow(ResponseException::notFound);
        var def = storageProviderDefinitionService
                .retrieveProviderDefinition(
                        provider.getStorageProviderDefinitionKey(),
                        provider.getStorageProviderDefinitionVersion()
                )
                .orElse(null);

        if (def == null) {
            return Map.of(
                    "success", false,
                    "error", "Der Speicheranbieter referenziert eine nicht vorhandene Anbieterdefinition."
            );
        }

        try {
            testConnection(provider, def, writable);
            return Map.of(
                    "success", true
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }

    private static <T> void testConnection(StorageProviderEntity provider, de.aivot.GoverBackend.storage.models.StorageProviderDefinition<T> definition, boolean writable) throws StorageException {
        T config;

        try {
            config = de.aivot.GoverBackend.elements.utils.ElementPOJOMapper
                    .mapToPOJO(provider.getConfiguration(), definition.getConfigClass());
        } catch (de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException e) {
            throw new StorageException(e, "Fehler beim Konvertieren der Speicheranbieter-Konfiguration.");
        }

        // Test write only if requested, because this may include the writing of test files (S3) and we do not want to always write unnecessary files
        definition.testConnection(config, writable);
    }
}
