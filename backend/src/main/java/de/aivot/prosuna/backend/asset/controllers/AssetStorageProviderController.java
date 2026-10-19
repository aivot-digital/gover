package de.aivot.prosuna.backend.asset.controllers;

import de.aivot.prosuna.backend.asset.dtos.AssetStorageProviderDTO;
import de.aivot.prosuna.backend.asset.permissions.AssetPermissionProvider;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.openApi.OpenApiConstants;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.storage.entities.StorageProviderEntity;
import de.aivot.prosuna.backend.storage.enums.StorageProviderType;
import de.aivot.prosuna.backend.storage.services.StorageProviderService;
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

import java.util.List;

@RestController
@RequestMapping("/api/assets/storage-providers/")
@Tag(
        name = OpenApiConstants.Tags.AssetsName,
        description = OpenApiConstants.Tags.AssetsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class AssetStorageProviderController {
    private final PermissionService permissionService;
    private final StorageProviderService storageProviderService;

    @Autowired
    public AssetStorageProviderController(
            PermissionService permissionService,
            StorageProviderService storageProviderService
    ) {
        this.permissionService = permissionService;
        this.storageProviderService = storageProviderService;
    }

    @GetMapping("")
    @Operation(
            summary = "List asset storage providers",
            description = "Lists storage providers that can be used in Dateien & Medien. " +
                    "Returns only asset UI metadata and requires the system-level permission `" +
                    AssetPermissionProvider.ASSET_READ + "`."
    )
    public List<AssetStorageProviderDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, AssetPermissionProvider.ASSET_READ);

        return storageProviderService
                .listAllByType(StorageProviderType.Assets)
                .stream()
                .map(AssetStorageProviderDTO::fromEntity)
                .toList();
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve asset storage provider metadata",
            description = "Retrieves asset UI metadata for a storage provider without exposing provider configuration. " +
                    "Requires the system-level permission `" + AssetPermissionProvider.ASSET_READ + "`."
    )
    public AssetStorageProviderDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, AssetPermissionProvider.ASSET_READ);

        return AssetStorageProviderDTO.fromEntity(getAssetStorageProvider(id));
    }

    @Nonnull
    private StorageProviderEntity getAssetStorageProvider(@Nonnull Integer id) throws ResponseException {
        var storageProvider = storageProviderService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
        if (storageProvider.getType() != StorageProviderType.Assets) {
            throw ResponseException.forbidden("Der angegebene Speicheranbieter ist kein Asset-Speicher.");
        }
        return storageProvider;
    }
}
