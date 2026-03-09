package de.aivot.GoverBackend.asset.controllers;

import de.aivot.GoverBackend.asset.dtos.AssetCreateRequestDTO;
import de.aivot.GoverBackend.asset.dtos.AssetMoveRequestDTO;
import de.aivot.GoverBackend.asset.dtos.AssetRequestDTO;
import de.aivot.GoverBackend.asset.entities.AssetEntity;
import de.aivot.GoverBackend.asset.entities.VStorageIndexItemWithAssetEntity;
import de.aivot.GoverBackend.asset.entities.VStorageIndexItemWithAssetEntityId;
import de.aivot.GoverBackend.asset.permissions.AssetPermissionProvider;
import de.aivot.GoverBackend.asset.repositories.AssetRepository;
import de.aivot.GoverBackend.asset.repositories.VStorageIndexItemWithAssetRepository;
import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.models.AuditLogPayload;
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
import de.aivot.GoverBackend.utils.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private static final String MODULE_NAME = "Dateien & Medien";

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
        this.auditService = auditService.createScopedAuditService(AssetController.class, MODULE_NAME);

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

    @GetMapping(value = "folders-tree/", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Retrieve full folder tree",
            description = "Retrieves the complete folder tree for the storage provider, starting at root. " +
                    "This endpoint is intended for folder selection UIs (e.g. copy/move destinations)."
    )
    public StorageFolder getFolderTree(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer storageProviderId
    ) throws ResponseException {
        permissionService
                .testSystemPermission(jwt, AssetPermissionProvider.ASSET_READ);

        var storageProvider = getStorageProvider(storageProviderId);

        return storageService
                .getFolderTreeFromIndex(storageProvider.getId());
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

        auditService
                .create()
                .withUser(execUser)
                .withAuditAction(
                        AuditAction.Create,
                        AssetEntity.class,
                        created.getPathFromRoot(),
                        "storagePathFromRoot",
                        Map.of(
                                "storageProviderId", storageProvider.getId(),
                                "isFolder", true
                        )
                )
                .withMessage(
                        "Der Ordner %s wurde von der Mitarbeiter:in %s erstellt.",
                        StringUtils.quote(created.getPathFromRoot()),
                        StringUtils.quote(execUser.getFullName())
                )
                .log();

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

        auditService
                .create()
                .withUser(execUser)
                .withAuditAction(
                        AuditAction.Delete,
                        AssetEntity.class,
                        folderPath,
                        "storagePathFromRoot",
                        Map.of(
                                "storageProviderId", storageProvider.getId(),
                                "isFolder", true
                        )
                )
                .withMessage(
                        "Der Ordner %s wurde von der Mitarbeiter:in %s gelöscht.",
                        StringUtils.quote(folderPath),
                        StringUtils.quote(execUser.getFullName())
                )
                .log();
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
        } catch (ResponseException e) {
            throw e;
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

        auditService
                .create()
                .withUser(execUser)
                .withAuditAction(
                        AuditAction.Create,
                        AssetEntity.class,
                        filePath,
                        "storagePathFromRoot",
                        Map.of(
                                "storageProviderId", storageProvider.getId(),
                                "isFolder", false
                        )
                )
                .withMessage(
                        "Die Datei %s wurde von der Mitarbeiter:in %s aktualisiert.",
                        StringUtils.quote(filePath),
                        StringUtils.quote(execUser.getFullName())
                )
                .log();

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
                .testSystemPermission(jwt, AssetPermissionProvider.ASSET_READ);

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
        var execUser = userService
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
            if (storageProvider.getReadOnlyStorage()) {
                throw ResponseException.badRequest("Der Speicheranbieter ist schreibgeschützt. Der Dateiinhalt kann nicht aktualisiert werden.");
            }

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

        var oldPrivate = asset.getPrivate();
        var newPrivate = updatedAsset.isPrivate() != null && updatedAsset.isPrivate();

        assetRepository
                .save(asset.setPrivate(newPrivate));

        auditService
                .create()
                .withUser(execUser)
                .withAuditAction(
                        AuditAction.Update,
                        AssetEntity.class,
                        filePath,
                        "storagePathFromRoot",
                        Map.of(
                                "storageProviderId", storageProvider.getId(),
                                "isFolder", false,
                                "fileContentUpdated", newAssetFile != null && !newAssetFile.isEmpty()
                        )
                )
                .withDiff(
                        Map.of("isPrivate", oldPrivate),
                        Map.of("isPrivate", newPrivate)
                )
                .withMessage(
                        "Die Datei %s wurde von der Mitarbeiter:in %s aktualisiert.",
                        StringUtils.quote(filePath),
                        StringUtils.quote(execUser.getFullName())
                )
                .log();

        return storageIndexItemWithAssetRepository
                .findById(VStorageIndexItemWithAssetEntityId.of(
                        asset.getStorageProviderId(),
                        asset.getStoragePathFromRoot()
                ))
                .orElseThrow(ResponseException::internalServerError);
    }

    @GetMapping("files-content/**")
    @Operation(
            summary = "Download an asset content",
            description = "Streams the file content of an asset for authenticated users."
    )
    public ResponseEntity<InputStreamResource> downloadFileContent(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer storageProviderId,
            @RequestParam(defaultValue = "true") boolean download,
            @Nonnull HttpServletRequest request
    ) throws ResponseException {
        permissionService
                .testSystemPermission(jwt, AssetPermissionProvider.ASSET_READ);

        var storageProvider = getStorageProvider(storageProviderId);

        var filePath = getNormalizedFileContentPath(request);

        var storageIndexItem = storageIndexItemWithAssetRepository
                .findById(VStorageIndexItemWithAssetEntityId.of(
                        storageProvider.getId(),
                        filePath
                ))
                .orElseThrow(ResponseException::notFound);

        var inputStream = storageService
                .getDocumentContent(storageProvider.getId(), filePath);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(storageIndexItem.getMimeType());
        } catch (InvalidMediaTypeException e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        var filename = storageIndexItem.getFilename();
        if (filename == null || filename.isBlank()) {
            filename = filePath.substring(filePath.lastIndexOf('/') + 1);
        }

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok().contentType(mediaType);
        var contentDispositionType = download ? "attachment" : "inline";
        var contentDisposition = ContentDisposition
                .builder(contentDispositionType)
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        responseBuilder.header("Content-Disposition", contentDisposition.toString());

        return responseBuilder.body(new InputStreamResource(inputStream));
    }

    @PostMapping(
            value = "move-file/",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Move an asset",
            description = "Move an asset file from a source path to a target path in the same storage provider."
    )
    public VStorageIndexItemWithAssetEntity moveFile(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer storageProviderId,
            @Nonnull @Valid @RequestBody AssetMoveRequestDTO moveRequest
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .testSystemPermission(user.getId(), AssetPermissionProvider.ASSET_UPDATE);

        var storageProvider = getStorageProvider(storageProviderId);
        if (storageProvider.getReadOnlyStorage()) {
            throw ResponseException.badRequest("Der Speicheranbieter ist schreibgeschützt. Die Datei kann nicht verschoben werden.");
        }

        var sourcePath = normalizeFilePathInput(moveRequest.sourcePath());
        var targetPath = normalizeFilePathInput(moveRequest.targetPath());

        if (sourcePath.equals(targetPath)) {
            return storageIndexItemWithAssetRepository
                    .findById(VStorageIndexItemWithAssetEntityId.of(
                            storageProvider.getId(),
                            sourcePath
                    ))
                    .orElseThrow(ResponseException::internalServerError);
        }

        var conflictingAsset = assetRepository
                .findByStorageProviderIdAndStoragePathFromRoot(storageProvider.getId(), targetPath);
        if (conflictingAsset.isPresent()) {
            throw ResponseException.conflict("Am Zielpfad existiert bereits eine Datei.");
        }

        // After the move, there is no need to update the asset because the asset path from root is updated automatically by the database because of the foreign key reference.
        var movedDocument = storageService.moveDocument(storageProvider.getId(), sourcePath, targetPath);

        auditService
                .create()
                .withUser(user)
                .withAuditAction(
                        AuditAction.Update,
                        AssetEntity.class,
                        sourcePath,
                        "storagePathFromRoot",
                        Map.of(
                                "storageProviderId", storageProvider.getId(),
                                "isFolder", false
                        )
                )
                .withDiff(
                        Map.of("storagePathFromRoot", sourcePath),
                        Map.of("storagePathFromRoot", targetPath)
                )
                .withMessage(
                        "Die Datei %s wurde von der Mitarbeiter:in %s nach %s verschoben.",
                        StringUtils.quote(sourcePath),
                        StringUtils.quote(user.getFullName()),
                        StringUtils.quote(targetPath)
                )
                .log();

        return storageIndexItemWithAssetRepository
                .findById(VStorageIndexItemWithAssetEntityId.of(
                        storageProvider.getId(),
                        movedDocument.getPathFromRoot()
                ))
                .orElseThrow(ResponseException::internalServerError);
    }

    @PostMapping(
            value = "copy-file/",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Copy an asset",
            description = "Copy an asset file from a source path to a target path in the same storage provider."
    )
    public VStorageIndexItemWithAssetEntity copyFile(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer storageProviderId,
            @Nonnull @Valid @RequestBody AssetMoveRequestDTO copyRequest
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .testSystemPermission(user.getId(), AssetPermissionProvider.ASSET_UPDATE);

        var storageProvider = getStorageProvider(storageProviderId);
        if (storageProvider.getReadOnlyStorage()) {
            throw ResponseException.badRequest("Der Speicheranbieter ist schreibgeschützt. Die Datei kann nicht kopiert werden.");
        }

        var sourcePath = normalizeFilePathInput(copyRequest.sourcePath());
        var targetPath = normalizeFilePathInput(copyRequest.targetPath());

        var sourceAsset = assetRepository
                .findByStorageProviderIdAndStoragePathFromRoot(storageProvider.getId(), sourcePath)
                .orElseThrow(ResponseException::notFound);

        if (sourcePath.equals(targetPath)) {
            throw ResponseException.conflict("Quell- und Zielpfad dürfen nicht identisch sein.");
        }

        var conflictingAsset = assetRepository
                .findByStorageProviderIdAndStoragePathFromRoot(storageProvider.getId(), targetPath);
        if (conflictingAsset.isPresent()) {
            throw ResponseException.conflict("Am Zielpfad existiert bereits eine Datei.");
        }

        var copiedDocument = storageService.copyDocument(storageProvider.getId(), sourcePath, targetPath);

        var copiedAsset = new AssetEntity()
                .setKey(UUID.randomUUID())
                .setUploaderId(user.getId())
                .setPrivate(sourceAsset.getPrivate())
                .setStorageProviderId(storageProvider.getId())
                .setStoragePathFromRoot(copiedDocument.getPathFromRoot());
        assetRepository.save(copiedAsset);

        auditService
                .create()
                .withUser(user)
                .withAuditAction(
                        AuditAction.Create,
                        AssetEntity.class,
                        targetPath,
                        "storagePathFromRoot",
                        Map.of(
                                "storageProviderId", storageProvider.getId(),
                                "isFolder", false,
                                "copyOf", sourcePath
                        )
                )
                .withMessage(
                        "Die Datei %s wurde von der Mitarbeiter:in %s nach %s kopiert.",
                        StringUtils.quote(sourcePath),
                        StringUtils.quote(user.getFullName()),
                        StringUtils.quote(targetPath)
                )
                .log();

        return storageIndexItemWithAssetRepository
                .findById(VStorageIndexItemWithAssetEntityId.of(
                        storageProvider.getId(),
                        copiedDocument.getPathFromRoot()
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

        auditService
                .create()
                .withUser(user)
                .withAuditAction(
                        AuditAction.Delete,
                        AssetEntity.class,
                        filePath,
                        "storagePathFromRoot",
                        Map.of(
                                "storageProviderId", storageProvider.getId(),
                                "isFolder", false
                        )
                )
                .withMessage(
                        "Die Datei %s wurde von der Mitarbeiter:in %s gelöscht.",
                        StringUtils.quote(filePath),
                        StringUtils.quote(user.getFullName())
                )
                .log();
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
    private static String getNormalizedFileContentPath(@Nonnull HttpServletRequest request) throws ResponseException {
        var requestUrl = request
                .getRequestURL()
                .toString();

        var marker = "/files-content/";
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
    private static String normalizeFilePathInput(@Nonnull String path) throws ResponseException {
        var normalizedPath = path.trim();
        if (normalizedPath.isBlank()) {
            throw ResponseException.notAcceptable("Der Pfad einer Datei darf nicht leer sein.");
        }
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        if (normalizedPath.endsWith("/")) {
            throw ResponseException.notAcceptable("Der Pfad einer Datei darf nicht mit einem Schrägstrich (/) enden.");
        }
        return normalizedPath;
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
