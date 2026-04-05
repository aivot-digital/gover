package de.aivot.GoverBackend.asset.controllers;

import de.aivot.GoverBackend.asset.entities.VStorageIndexItemWithAssetEntity;
import de.aivot.GoverBackend.asset.permissions.AssetPermissionProvider;
import de.aivot.GoverBackend.asset.repositories.VStorageIndexItemWithAssetRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.permissions.services.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/assets-by-key/{key}/")
@Tag(
        name = OpenApiConstants.Tags.AssetsName,
        description = OpenApiConstants.Tags.AssetsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class KeyBasedAssetController {
    private static final String MODULE_NAME = "Dateien & Medien";

    private final PermissionService permissionService;
    private final VStorageIndexItemWithAssetRepository storageIndexItemWithAssetRepository;

    @Autowired
    public KeyBasedAssetController(PermissionService permissionService,
                                   VStorageIndexItemWithAssetRepository storageIndexItemWithAssetRepository) {
        this.permissionService = permissionService;
        this.storageIndexItemWithAssetRepository = storageIndexItemWithAssetRepository;
    }

    @GetMapping("")
    @Operation(
            summary = "Find a asset by its key",
            description = "Find a single asset by its key"
    )
    public VStorageIndexItemWithAssetEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID key
    ) throws ResponseException {
        permissionService
                .testSystemPermission(jwt, AssetPermissionProvider.ASSET_READ);

        return storageIndexItemWithAssetRepository
                .findByAssetKey(key)
                .orElseThrow(ResponseException::notFound);
    }
}
