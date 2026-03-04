package de.aivot.GoverBackend.asset.controllers;

import de.aivot.GoverBackend.asset.dtos.AssetCreateRequestDTO;
import de.aivot.GoverBackend.asset.dtos.AssetRequestDTO;
import de.aivot.GoverBackend.asset.entities.AssetEntity;
import de.aivot.GoverBackend.asset.entities.VStorageIndexItemWithAssetEntity;
import de.aivot.GoverBackend.asset.entities.VStorageIndexItemWithAssetEntityId;
import de.aivot.GoverBackend.asset.permissions.AssetPermissionProvider;
import de.aivot.GoverBackend.asset.repositories.AssetRepository;
import de.aivot.GoverBackend.asset.repositories.VStorageIndexItemWithAssetRepository;
import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.permissions.services.PermissionService;
import de.aivot.GoverBackend.storage.entities.StorageProviderEntity;
import de.aivot.GoverBackend.storage.enums.StorageProviderType;
import de.aivot.GoverBackend.storage.models.StorageDocument;
import de.aivot.GoverBackend.storage.models.StorageFolder;
import de.aivot.GoverBackend.storage.models.StorageItemMetadata;
import de.aivot.GoverBackend.storage.services.StorageProviderService;
import de.aivot.GoverBackend.storage.services.StorageService;
import de.aivot.GoverBackend.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/assets/{storageProviderId}/")
@Tag(
        name = OpenApiConstants.Tags.AssetsName,
        description = OpenApiConstants.Tags.AssetsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class AssetController {
    private final ScopedAuditService auditService;

    private final UserService userService;
    private final PermissionService permissionService;
    private final StorageProviderService storageProviderService;
    private final VStorageIndexItemWithAssetRepository storageIndexItemWithAssetRepository;
    private final StorageService storageService;
    private final AssetRepository assetRepository;

    @Autowired
    public AssetController(AuditService auditService,
                           UserService userService,
                           PermissionService permissionService,
                           StorageProviderService storageProviderService,
                           VStorageIndexItemWithAssetRepository storageIndexItemWithAssetRepository,
                           StorageService storageService,
                           AssetRepository assetRepository) {
        this.auditService = auditService.createScopedAuditService(AssetController.class);

        this.userService = userService;
        this.permissionService = permissionService;
        this.storageProviderService = storageProviderService;
        this.storageIndexItemWithAssetRepository = storageIndexItemWithAssetRepository;
        this.storageService = storageService;
        this.assetRepository = assetRepository;
    }

    // region Folders

    @GetMapping(value = {
            "folders/",
            "folders/**",
    }, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "List assets in a folder",
            description = "Retrieves the list of all items in a specified folder path. This includes files as well as subfolders. " +
                    "You can use this endpoint to navigate trough the filesystem for a specific storage provider."
    )
    public List<VStorageIndexItemWithAssetEntity> listFolderContent(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer storageProviderId,
            @Nonnull HttpServletRequest request
    ) throws ResponseException {
        permissionService
                .testSystemPermission(jwt, AssetPermissionProvider.ASSET_READ);

        var storageProvider = getStorageProvider(storageProviderId);

        var normalizedPath = getNormalizedFolderPath(request);

        return storageIndexItemWithAssetRepository
                .listAllInFolder(storageProvider.getId(), "^" + normalizedPath + "([^/]+$|[^/]+/$)", false);
    }

    @PostMapping(
            value = "folders/**",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Create a new folder",
            description = "Create a new folder at the given path in the storage provider. " +
                    "You can use this to create new folders in the filesystem for assets to be placed in."
    )
    public StorageFolder createFolder(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer storageProviderId,
            @Nonnull HttpServletRequest request
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .testSystemPermission(execUser.getId(), AssetPermissionProvider.ASSET_CREATE);

        var storageProvider = getStorageProvider(storageProviderId);

        var folderPath = getNormalizedFolderPath(request);

        var created = storageService.createFolder(storageProvider.getId(), folderPath);

        auditService.logAction(execUser, AuditAction.Create, AssetEntity.class, Map.of(
                "folder", true,
                "path", folderPath
        ));

        return created;
    }

    @DeleteMapping(
            value = "folders/**"
    )
    @Operation(
            summary = "Delete a folder",
            description = "Delete the folder at the given path. " +
                    "This will delete this folder, as well as all subfolders and assets in the subfolders."
    )
    public void deleteFolder(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer storageProviderId,
            @Nonnull HttpServletRequest request
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .testSystemPermission(execUser.getId(), AssetPermissionProvider.ASSET_DELETE);

        var storageProvider = getStorageProvider(storageProviderId);

        var folderPath = getNormalizedFolderPath(request);

        storageService.deleteFolder(storageProvider.getId(), folderPath);

        assetRepository.deleteAllByStoragePathFromRootStartingWith(folderPath);

        auditService.logAction(execUser, AuditAction.Delete, AssetEntity.class, Map.of(
                "folder", true,
                "path", folderPath
        ));
    }

    // endregion

    // region Files

    @PostMapping(
            value = "files/**",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Create a new asset",
            description = "Upload a new asset file and create an asset record. " +
                    "This stores the file in the given storage provider, registers a storage index item and creates a new asset object."
    )
    public VStorageIndexItemWithAssetEntity createFile(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer storageProviderId,
            @Nonnull @RequestPart(value = "file", required = true) MultipartFile newAssetFile,
            @Nonnull @Valid @RequestPart(value = "data", required = true) AssetCreateRequestDTO newAsset,
            @Nonnull HttpServletRequest request
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .testSystemPermission(execUser.getId(), AssetPermissionProvider.ASSET_CREATE);

        var filePath = getNormalizedFilePath(request);

        var storageProvider = getStorageProvider(storageProviderId);

        StorageDocument storedDocument;
        try {
            storedDocument = storageService
                    .storeDocument(storageProvider.getId(),
                            filePath,
                            newAssetFile.getInputStream(),
                            newAsset.metadata() != null ? newAsset.metadata() : StorageItemMetadata.empty());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        var asset = new AssetEntity()
                .setKey(UUID.randomUUID())
                .setUploaderId(execUser.getId())
                .setPrivate(newAsset.isPrivate() != null && newAsset.isPrivate())
                .setStorageProviderId(storageProvider.getId())
                .setStoragePathFromRoot(storedDocument.getPathFromRoot());
        assetRepository.save(asset);

        return storageIndexItemWithAssetRepository
                .findById(VStorageIndexItemWithAssetEntityId.of(
                        asset.getStorageProviderId(),
                        asset.getStoragePathFromRoot()
                ))
                .orElseThrow(ResponseException::internalServerError);
    }

    @GetMapping(
            value = "files/**",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Retrieve an asset",
            description = "Retrieve details of a specific asset by its path and storage provider."
    )
    public VStorageIndexItemWithAssetEntity retrieveFile(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer storageProviderId,
            @Nonnull HttpServletRequest request
    ) throws ResponseException {
        permissionService
                .testSystemPermission(jwt, AssetPermissionProvider.ASSET_CREATE);

        var storageProvider = getStorageProvider(storageProviderId);

        var filePath = getNormalizedFilePath(request);

        return storageIndexItemWithAssetRepository
                .findById(VStorageIndexItemWithAssetEntityId.of(
                        storageProvider.getId(),
                        filePath
                ))
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping(
            value = "files/**",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Update an asset",
            description = "Update the details of an existing asset. " +
                    "Note that the filename cannot be changed by this endpoint, because this needs a move."
    )
    public VStorageIndexItemWithAssetEntity updateFile(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer storageProviderId,
            @Nullable @RequestPart(value = "file", required = false) MultipartFile newAssetFile,
            @Nonnull @Valid @RequestPart(value = "data", required = true) AssetRequestDTO updatedAsset,
            @Nonnull HttpServletRequest request
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .testSystemPermission(jwt, AssetPermissionProvider.ASSET_UPDATE);

        var storageProvider = getStorageProvider(storageProviderId);

        var filePath = getNormalizedFilePath(request);

        var asset = assetRepository
                .findByStorageProviderIdAndStoragePathFromRoot(storageProvider.getId(), filePath)
                .orElseThrow(ResponseException::notFound);

        if (newAssetFile != null && !newAssetFile.isEmpty()) {
            try {
                storageService
                        .storeDocument(
                                storageProvider.getId(),
                                filePath,
                                newAssetFile.getInputStream(),
                                updatedAsset.metadata() != null ? updatedAsset.metadata() : StorageItemMetadata.empty()
                        );
            } catch (Exception e) {
                throw ResponseException.badRequest("Fehler beim Speichern des Datei-Inhalts.");
            }
        }

        assetRepository
                .save(asset.setPrivate(updatedAsset.isPrivate() != null && updatedAsset.isPrivate()));

        auditService.logAction(user, AuditAction.Update, AssetEntity.class, Map.of(
                "folder", false,
                "path", filePath
        ));

        return storageIndexItemWithAssetRepository
                .findById(VStorageIndexItemWithAssetEntityId.of(
                        asset.getStorageProviderId(),
                        asset.getStoragePathFromRoot()
                ))
                .orElseThrow(ResponseException::internalServerError);
    }

    @DeleteMapping("files/**")
    @Operation(
            summary = "Delete an asset",
            description = "Delete a specific asset by its path and storage provider. " +
                    "This will remove both the asset record and the associated file from storage."
    )
    public void deleteFile(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer storageProviderId,
            @Nonnull HttpServletRequest request
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .testSystemPermission(user.getId(), AssetPermissionProvider.ASSET_DELETE);

        var storageProvider = getStorageProvider(storageProviderId);

        var filePath = getNormalizedFilePath(request);

        storageService.deleteDocument(storageProvider.getId(), filePath);
    }

    // endregion

    // region Utilities

    @Nonnull
    private static String getNormalizedFolderPath(@Nonnull HttpServletRequest request) throws ResponseException {
        var requestUrl = request
                .getRequestURL()
                .toString();

        var marker = "/folders/";
        var markerIndex = requestUrl.indexOf(marker);
        if (markerIndex < 0) {
            return "/";
        }

        var normalizedPath = requestUrl.substring(markerIndex + marker.length());
        if (normalizedPath.isBlank()) {
            return "/";
        }

        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }

        if (!normalizedPath.endsWith("/")) {
            throw ResponseException.notAcceptable("Der Pfad eines Ordners muss mit einem Schrägstrich (/) enden.");
        }

        return URLDecoder.decode(normalizedPath, StandardCharsets.UTF_8);
    }

    @Nonnull
    private static String getNormalizedFilePath(@Nonnull HttpServletRequest request) throws ResponseException {
        var requestUrl = request
                .getRequestURL()
                .toString();

        var marker = "/files/";
        var markerIndex = requestUrl.indexOf(marker);
        if (markerIndex < 0) {
            throw ResponseException.notAcceptable("Der Root eines Speichers kann keine Datei sein.");
        }

        var normalizedPath = requestUrl.substring(markerIndex + marker.length());
        if (normalizedPath.isBlank()) {
            throw ResponseException.notAcceptable("Der Root eines Speichers kann keine Datei sein.");
        }

        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }

        if (normalizedPath.endsWith("/")) {
            throw ResponseException.notAcceptable("Der Pfad einer Datei darf nicht mit einem Schrägstrich (/) enden.");
        }

        return URLDecoder.decode(normalizedPath, StandardCharsets.UTF_8);
    }

    @Nonnull
    private StorageProviderEntity getStorageProvider(@Nonnull Integer storageProviderId) throws ResponseException {
        var storageProvider = storageProviderService
                .retrieve(storageProviderId)
                .orElseThrow(ResponseException::notFound);
        if (storageProvider.getType() != StorageProviderType.Assets) {
            throw ResponseException.forbidden("Der angegebene Speicheranbieter ist kein Asset-Speicher.");
        }
        return storageProvider;
    }

    // endregion
}
