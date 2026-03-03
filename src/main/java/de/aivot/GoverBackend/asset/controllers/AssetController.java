package de.aivot.GoverBackend.asset.controllers;

import de.aivot.GoverBackend.asset.dtos.AssetRequestDTO;
import de.aivot.GoverBackend.asset.dtos.AssetResponseDTO;
import de.aivot.GoverBackend.asset.entities.AssetEntity;
import de.aivot.GoverBackend.asset.filters.AssetFilter;
import de.aivot.GoverBackend.asset.repositories.AssetRepository;
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
import de.aivot.GoverBackend.user.services.UserService;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/assets/")
@Tag(
        name = OpenApiConstants.Tags.AssetsName,
        description = OpenApiConstants.Tags.AssetsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class AssetController {
    private final ScopedAuditService auditService;

    private final AssetService assetService;
    private final AssetRepository assetRepository;
    private final AVService avService;
    private final UserService userService;
    private final PermissionService permissionService;

    @Autowired
    public AssetController(AuditService auditService,
                           AssetService assetService,
                           AssetRepository assetRepository,
                           AVService avService,
                           UserService userService,
                           PermissionService permissionService) {
        this.auditService = auditService.createScopedAuditService(AssetController.class);

        this.assetService = assetService;
        this.assetRepository = assetRepository;
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
        return assetService
                .list(pageable, filter)
                .map(AssetResponseDTO::fromEntity);
    }

    @PostMapping(
            value = "",
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
            @Nonnull @RequestPart(value = "file", required = true) MultipartFile file,
            @Nullable @RequestPart(value = "filename", required = false) String explicitFilename,
            @Nullable @RequestPart(value = "private", required = false) Boolean privateAsset
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .hasSystemPermissionThrows(execUser, Permissions.ASSET_CREATE);

        avService.testFile(file);

        String filename = explicitFilename != null ? explicitFilename : file.getOriginalFilename();

        if (filename == null) {
            throw ResponseException.badRequest("Der Dateiname konnte nicht ermittelt werden.");
        }

        var asset = new AssetEntity();
        asset.setFilename(filename);
        asset.setCreated(LocalDateTime.now());
        asset.setUploaderId(execUser.getId());
        asset.setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        asset.setPrivate(privateAsset != null ? privateAsset : true);
        try {
            asset.setFileBytes(file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var createdAsset = assetService.create(asset);

        auditService.logAction(execUser, AuditAction.Create, AssetEntity.class, Map.of(
                "key", createdAsset.getKey(),
                "filename", createdAsset.getFilename()
        ));

        return AssetResponseDTO.fromEntity(createdAsset);
    }

    @GetMapping(
            value = "{assetId}/",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Retrieve an asset",
            description = "Retrieve details of a specific asset by its ID."
    )
    public AssetResponseDTO retrieve(
            @Nonnull @PathVariable UUID assetId
    ) throws ResponseException {
        return assetService
                .retrieve(assetId)
                .map(AssetResponseDTO::fromEntity)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping(
            value = "{assetId}/",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Update an asset",
            description = "Update the details of an existing asset. " +
                    "Note that the actual file content cannot be changed through this endpoint."
    )
    public AssetResponseDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID assetId,
            @Nonnull @RequestBody @Valid AssetRequestDTO requestDTO
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var updatedAsset = assetService
                .update(assetId, requestDTO.toEntity());

        auditService.logAction(user, AuditAction.Update, AssetEntity.class, Map.of(
                "key", updatedAsset.getKey(),
                "filename", updatedAsset.getFilename(),
                "isPrivate", updatedAsset.getPrivate()
        ));

        return AssetResponseDTO
                .fromEntity(updatedAsset);
    }

    @DeleteMapping("{assetId}/")
    @Operation(
            summary = "Delete an asset",
            description = "Delete a specific asset by its ID. " +
                    "This will remove both the asset record and the associated file from storage."
    )
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID assetId
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var asset = assetRepository
                .findById(assetId)
                .orElseThrow(ResponseException::notFound);

        auditService.logAction(user, AuditAction.Delete, AssetEntity.class, Map.of(
                "key", asset.getKey(),
                "filename", asset.getFilename()
        ));

        assetService.delete(assetId);
    }
}
