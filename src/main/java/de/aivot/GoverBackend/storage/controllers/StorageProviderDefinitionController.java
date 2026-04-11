package de.aivot.GoverBackend.storage.controllers;

import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.storage.models.StorageProviderDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// TODO: Do we need permissions here?

@RestController
@RequestMapping("/api/storage-provider-definitions/")
@Tag(
        name = OpenApiConstants.Tags.StorageProvidersName,
        description = OpenApiConstants.Tags.StorageProvidersDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class StorageProviderDefinitionController {
    private final List<StorageProviderDefinition<?>> storageProviderDefinitions;

    @Autowired
    public StorageProviderDefinitionController(List<StorageProviderDefinition<?>> storageProviderDefinitions) {
        this.storageProviderDefinitions = storageProviderDefinitions;
    }

    @GetMapping("")
    @Operation(
            summary = "List Storage Provider Definitions",
            description = "Retrieve a list of all available storage provider definitions."
    )
    public List<StorageProviderDefinitionDTO> list() throws ResponseException {
        return storageProviderDefinitions
                .stream()
                .map(StorageProviderDefinitionDTO::from)
                .toList();
    }

    @GetMapping("{key}/{version}/")
    @Operation(
            summary = "Retrieve Storage Provider Definition",
            description = "Retrieve a specific storage provider definition by its key and version."
    )
    public StorageProviderDefinitionDTO retrieve(
            @Nonnull @PathVariable String key,
            @Nonnull @PathVariable Integer version
    ) throws ResponseException {
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
