package de.aivot.GoverBackend.storage.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.permissions.services.PermissionService;
import de.aivot.GoverBackend.storage.entities.StorageIndexItemEntity;
import de.aivot.GoverBackend.storage.entities.StorageIndexItemEntityId;
import de.aivot.GoverBackend.storage.entities.StorageProviderEntity;
import de.aivot.GoverBackend.storage.enums.StorageProviderStatus;
import de.aivot.GoverBackend.storage.filters.StorageProviderFilter;
import de.aivot.GoverBackend.storage.models.StorageFolder;
import de.aivot.GoverBackend.storage.models.StorageProviderDefinition;
import de.aivot.GoverBackend.storage.permissions.StoragePermissionProvider;
import de.aivot.GoverBackend.storage.repositories.StorageIndexItemRepository;
import de.aivot.GoverBackend.storage.services.StorageProviderService;
import de.aivot.GoverBackend.storage.services.StorageService;
import de.aivot.GoverBackend.storage.services.StorageSyncWorker;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.utils.StringUtils;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private final StorageService storageService;

    @Autowired
    public StorageProviderController(AuditService auditService,
                                     UserService userService,
                                     StorageProviderService storageProviderService,
                                     PermissionService permissionService,
                                     RabbitTemplate rabbitTemplate,
                                     StorageIndexItemRepository storageIndexItemRepository, StorageService storageService) {
        this.auditService = auditService.createScopedAuditService(StorageProviderController.class);
        this.userService = userService;
        this.storageProviderService = storageProviderService;
        this.permissionService = permissionService;
        this.rabbitTemplate = rabbitTemplate;
        this.storageIndexItemRepository = storageIndexItemRepository;
        this.storageService = storageService;
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

        auditService
                .logAction(execUser, AuditAction.Create, StorageProviderEntity.class, Map.of(
                        "id", created.getId(),
                        "name", created.getName()
                ));

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

        auditService
                .logAction(execUser, AuditAction.Update, StorageProviderEntity.class, Map.of(
                        "id", result.getId(),
                        "name", result.getName()
                ));

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

        auditService
                .logAction(execUser, AuditAction.Delete, StorageProviderEntity.class, Map.of(
                        "id", deleted.getId(),
                        "name", deleted.getName()
                ));
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

        auditService
                .logAction(execUser, AuditAction.Update, StorageProviderEntity.class, Map.of(
                        "id", updated.getId(),
                        "name", updated.getName(),
                        "resynced", true
                ));

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
                .listAllInFolder(id, "^" + normalizedPath + "([^/]+$|[^/]+/$)");
    }

    @GetMapping(value = {
            "{id}/files/",
            "{id}/files/**",
    })
    @Operation(
            summary = "Get Folder from Storage Provider",
            description = "Retrieve a folder from the specified storage provider. Requires the permission " + StoragePermissionProvider.STORAGE_PROVIDER_READ + "."
    )
    public ResponseEntity<Resource> getDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id,
            @RequestParam(defaultValue = "false") boolean download,
            @Nonnull HttpServletRequest request
    ) throws ResponseException {
        permissionService
                .testSystemPermission(jwt, StoragePermissionProvider.STORAGE_PROVIDER_READ);

        var normalizedPath = getNormalizedPath(request, false);

        var storageItem = storageIndexItemRepository
                .findByStorageProviderIdAndPathFromRootAndIsDirectoryIsFalse(
                        id,
                        normalizedPath
                )
                .orElseThrow(ResponseException::notFound);

        var is = storageService
                .getDocumentContent(id, storageItem.getPathFromRoot());

        byte[] bytes;
        try {
            bytes = is.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Resource resource = new ByteArrayResource(bytes);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(storageItem.getMimeType());
        } catch (InvalidMediaTypeException e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        if (resource.exists() && resource.isReadable()) {
            ResponseEntity.BodyBuilder responseBuilder = ResponseEntity
                    .ok()
                    .contentType(mediaType);

            if (download) {
                responseBuilder.header("Content-Disposition", "attachment; filename=\"" + storageItem.getFilename() + "\"");
            }

            return responseBuilder.body(resource);
        }

        throw ResponseException.notFound();
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
}
