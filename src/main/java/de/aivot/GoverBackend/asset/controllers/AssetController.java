package de.aivot.GoverBackend.asset.controllers;

import de.aivot.GoverBackend.asset.dtos.AssetRequestDTO;
import de.aivot.GoverBackend.asset.dtos.AssetCreateRequestDTO;
import de.aivot.GoverBackend.asset.dtos.AssetResponseDTO;
import de.aivot.GoverBackend.asset.dtos.AssetFolderGroupResponseDTO;
import de.aivot.GoverBackend.asset.entities.AssetEntity;
import de.aivot.GoverBackend.asset.filters.AssetFilter;
import de.aivot.GoverBackend.asset.services.AssetService;
import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.permissions.data.Permissions;
import de.aivot.GoverBackend.permissions.services.PermissionService;
import de.aivot.GoverBackend.av.services.AVService;
import de.aivot.GoverBackend.storage.models.StorageItemMetadata;
import de.aivot.GoverBackend.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/assets/")
@Tag(
        name = OpenApiConstants.Tags.AssetsName,
        description = OpenApiConstants.Tags.AssetsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class AssetController {
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ScopedAuditService auditService;

    private final AssetService assetService;
    private final AVService avService;
    private final UserService userService;
    private final PermissionService permissionService;

    @Autowired
    public AssetController(AuditService auditService,
                           AssetService assetService,
                           AVService avService,
                           UserService userService,
                           PermissionService permissionService) {
        this.auditService = auditService.createScopedAuditService(AssetController.class);

        this.assetService = assetService;
        this.avService = avService;
        this.userService = userService;
        this.permissionService = permissionService;
    }

    @GetMapping(
            value = "",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "List all assets",
            description = "Retrieve a paginated list of assets with optional filtering."
    )
    public Page<AssetResponseDTO> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid AssetFilter filter
    ) throws ResponseException {
        return assetService.listWithMetadata(pageable, filter);
    }

    @GetMapping(value = {
            "folders/",
            "folders/**",
    }, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "List assets grouped by storage provider and folder",
            description = "Retrieves folder entries from the storage index and file entries from asset entities, grouped by storage provider."
    )
    public List<AssetFolderGroupResponseDTO> listGroupedByFolder(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .hasSystemPermissionThrows(user, Permissions.ASSET_READ);

        var normalizedPath = getNormalizedFolderPath(request);
        return assetService.listGroupedByStorageProvider(normalizedPath);
    }

    @PostMapping(
            value = "path/**",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Create a new asset",
            description = "Upload a new asset file and create an asset record. " +
                    "The uploaded file will be scanned for viruses before being stored."
    )
    public AssetResponseDTO create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull HttpServletRequest request,
            @Nonnull @RequestPart(value = "file", required = true) MultipartFile file,
            @Nonnull @Valid @RequestPart(value = "data", required = true) AssetCreateRequestDTO requestDTO,
            @Nonnull @RequestParam(value = "storageProviderId", required = true) Integer storageProviderId
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .hasSystemPermissionThrows(execUser, Permissions.ASSET_CREATE);

        avService.testFile(file);

        var storagePathFromRoot = getNormalizedAssetPath(request);
        ensureMatchingPath(requestDTO.storagePathFromRoot(), storagePathFromRoot);

        String filename = file.getOriginalFilename();

        if (filename == null) {
            throw ResponseException.badRequest("Der Dateiname konnte nicht ermittelt werden.");
        }

        var contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        var isPrivate = requestDTO.isPrivate();
        var metadata = requestDTO.metadata() != null
                ? requestDTO.metadata()
                : StorageItemMetadata.empty();

        var asset = new AssetEntity();
        asset.setStoragePathFromRoot(storagePathFromRoot);
        asset.setFilename(filename);
        asset.setCreated(LocalDateTime.now());
        asset.setUploaderId(execUser.getId());
        asset.setContentType(contentType);
        asset.setPrivate(isPrivate);

        try {
            var createdAssetResponse = assetService.createWithResponse(asset, file.getInputStream(), storageProviderId, metadata);

            auditService.logAction(execUser, AuditAction.Create, AssetEntity.class, Map.of(
                    "key", createdAssetResponse.key(),
                    "filename", createdAssetResponse.filename()
            ));

            return createdAssetResponse;
        } catch (IOException e) {
            throw ResponseException.internalServerError(e, "Die Dateidaten des Uploads konnten nicht gelesen werden.");
        }
    }

    @GetMapping(
            value = "path/**",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Retrieve an asset",
            description = "Retrieve details of a specific asset by its path and storage provider."
    )
    public AssetResponseDTO retrieve(
            @Nonnull HttpServletRequest request,
            @Nonnull @RequestParam(value = "storageProviderId", required = true) Integer storageProviderId
    ) throws ResponseException {
        var storagePathFromRoot = getNormalizedAssetPath(request);
        return assetService
                .retrieveResponseByPath(storageProviderId, storagePathFromRoot)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping(
            value = "path/**",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Update an asset",
            description = "Update the details of an existing asset. " +
                    "Note that the actual file content cannot be changed through this endpoint."
    )
    public AssetResponseDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull HttpServletRequest request,
            @Nonnull @RequestBody @Valid AssetRequestDTO requestDTO,
            @Nonnull @RequestParam(value = "storageProviderId", required = true) Integer storageProviderId
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var storagePathFromRoot = getNormalizedAssetPath(request);
        ensureMatchingPath(requestDTO.storagePathFromRoot(), storagePathFromRoot);
        var updatedAssetResponse = assetService
                .updateByPath(storageProviderId, storagePathFromRoot, requestDTO.toEntity(), requestDTO.metadata());

        auditService.logAction(user, AuditAction.Update, AssetEntity.class, Map.of(
                "key", updatedAssetResponse.key(),
                "filename", updatedAssetResponse.filename(),
                "isPrivate", updatedAssetResponse.isPrivate()
        ));

        return updatedAssetResponse;
    }

    @DeleteMapping("path/**")
    @Operation(
            summary = "Delete an asset",
            description = "Delete a specific asset by its path and storage provider. " +
                    "This will remove both the asset record and the associated file from storage."
    )
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull HttpServletRequest request,
            @Nonnull @RequestParam(value = "storageProviderId", required = true) Integer storageProviderId
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var storagePathFromRoot = getNormalizedAssetPath(request);
        var assetResponse = assetService.deleteByPath(storageProviderId, storagePathFromRoot);

        auditService.logAction(user, AuditAction.Delete, AssetEntity.class, Map.of(
                "key", assetResponse.key(),
                "filename", assetResponse.filename()
        ));
    }

    @Nonnull
    private static String getNormalizedFolderPath(@Nonnull HttpServletRequest request) {
        var normalizedPath = extractNormalizedPathFromRequest(request, "folders");
        if (!normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath + "/";
        }
        return normalizedPath;
    }

    @Nonnull
    private static String getNormalizedAssetPath(@Nonnull HttpServletRequest request) throws ResponseException {
        var normalizedPath = extractNormalizedPathFromRequest(request, "path");
        if (normalizedPath.equals("/")) {
            throw ResponseException.badRequest("Der Pfad der Asset-Datei darf nicht leer sein.");
        }
        return normalizedPath;
    }

    @Nonnull
    private static String extractNormalizedPathFromRequest(@Nonnull HttpServletRequest request,
                                                           @Nonnull String marker) {
        var bestMatchingPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        var pathWithinHandlerMapping = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String extractedPath = null;

        if (bestMatchingPattern != null && pathWithinHandlerMapping != null) {
            extractedPath = pathMatcher.extractPathWithinPattern(bestMatchingPattern, pathWithinHandlerMapping);
        }

        if (extractedPath == null) {
            var requestUri = request.getRequestURI();
            var markerWithSlash = "/" + marker + "/";
            var markerIndex = requestUri.indexOf(markerWithSlash);
            if (markerIndex >= 0) {
                extractedPath = requestUri.substring(markerIndex + markerWithSlash.length());
            }
        }

        var normalizedPath = "/";
        if (extractedPath != null && !extractedPath.isBlank()) {
            normalizedPath = extractedPath;
            if (!normalizedPath.startsWith("/")) {
                normalizedPath = "/" + normalizedPath;
            }
        }

        return URLDecoder.decode(normalizedPath, StandardCharsets.UTF_8);
    }

    private static void ensureMatchingPath(@Nonnull String requestBodyPath,
                                           @Nonnull String routePath) throws ResponseException {
        var normalizedBodyPath = requestBodyPath.trim();
        if (!normalizedBodyPath.startsWith("/")) {
            normalizedBodyPath = "/" + normalizedBodyPath;
        }

        if (!normalizedBodyPath.equals(routePath)) {
            throw ResponseException.badRequest("Der Pfad im Request-Body muss mit dem Pfad in der URL übereinstimmen.");
        }
    }
}
