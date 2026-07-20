package de.aivot.gover.backend.storage.controllers;

import de.aivot.gover.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.openApi.OpenApiConstants;
import de.aivot.gover.backend.permissions.services.PermissionService;
import de.aivot.gover.backend.storage.models.StorageProviderDefinition;
import de.aivot.gover.backend.storage.permissions.StoragePermissionProvider;
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
@RequestMapping("/api/storage-provider-definitions/")
@Tag(
        name = OpenApiConstants.Tags.StorageProvidersName,
        description = OpenApiConstants.Tags.StorageProvidersDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class StorageProviderDefinitionController {
    private final List<StorageProviderDefinition<?>> storageProviderDefinitions;
    private final PermissionService permissionService;

    @Autowired
    public StorageProviderDefinitionController(List<StorageProviderDefinition<?>> storageProviderDefinitions,
                                               PermissionService permissionService) {
        this.storageProviderDefinitions = storageProviderDefinitions;
        this.permissionService = permissionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Storage Provider Definitions",
            description = "Retrieve a list of all available storage provider definitions. " +
                    "This requires the permission „" + StoragePermissionProvider.STORAGE_PROVIDER_READ + "“."
    )
    public List<StorageProviderDefinitionDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt
    ) throws ResponseException {
        permissionService
                .hasSystemPermission(jwt, StoragePermissionProvider.STORAGE_PROVIDER_READ);

        return storageProviderDefinitions
                .stream()
                .map(StorageProviderDefinitionDTO::from)
                .toList();
    }

    @GetMapping("{key}/{version}/")
    @Operation(
            summary = "Retrieve Storage Provider Definition",
            description = "Retrieve a specific storage provider definition by its key and version. " +
                    "This requires the permission „" + StoragePermissionProvider.STORAGE_PROVIDER_READ + "“."
    )
    public StorageProviderDefinitionDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String key,
            @Nonnull @PathVariable Integer version
    ) throws ResponseException {
        permissionService
                .hasSystemPermission(jwt, StoragePermissionProvider.STORAGE_PROVIDER_READ);

        return storageProviderDefinitions
                .stream()
                .filter(def -> def.getKey().equals(key) && def.getMajorVersion().equals(version))
                .findFirst()
                .map(StorageProviderDefinitionDTO::from)
                .orElseThrow(ResponseException::notFound);
    }

    public record StorageProviderDefinitionDTO(
            @Nonnull String key,
            @Nonnull Integer version,
            @Nonnull String name,
            @Nonnull String description,
            @Nonnull Boolean supportsMetadataAttributes,
            @Nullable ConfigLayoutElement providerConfigLayout
    ) {

        public static StorageProviderDefinitionDTO from(StorageProviderDefinition<?> definition) {
            ConfigLayoutElement layout;
            try {
                layout = definition.getProviderConfigLayout();
            } catch (ResponseException e) {
                throw new RuntimeException(e);
            }

            return new StorageProviderDefinitionDTO(
                    definition.getKey(),
                    definition.getMajorVersion(),
                    definition.getName(),
                    definition.getDescription(),
                    definition.getSupportsMetadataAttributes(),
                    layout
            );
        }

    }
}
