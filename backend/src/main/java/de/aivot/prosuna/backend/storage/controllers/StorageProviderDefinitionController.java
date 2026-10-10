package de.aivot.prosuna.backend.storage.controllers;

import de.aivot.prosuna.backend.elements.dtos.ElementValuesDerivationRequestDTO;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.ElementDerivationRequest;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.services.ElementDerivationService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.openApi.OpenApiConstants;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.storage.models.StorageProviderDefinition;
import de.aivot.prosuna.backend.storage.permissions.StoragePermissionProvider;
import de.aivot.prosuna.backend.utils.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final ElementDerivationService elementDerivationService;

    @Autowired
    public StorageProviderDefinitionController(List<StorageProviderDefinition<?>> storageProviderDefinitions,
                                               PermissionService permissionService,
                                               ElementDerivationService elementDerivationService) {
        this.storageProviderDefinitions = storageProviderDefinitions;
        this.permissionService = permissionService;
        this.elementDerivationService = elementDerivationService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Storage Provider Definitions",
            description = "Retrieve a list of all available storage provider definitions. " +
                    "Requires at least one of the system-level permissions `" +
                    StoragePermissionProvider.STORAGE_PROVIDER_READ + "` or `" +
                    StoragePermissionProvider.STORAGE_PROVIDER_CREATE + "`."
    )
    public List<StorageProviderDefinitionDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt
    ) throws ResponseException {
        requireDefinitionAccess(jwt);

        return storageProviderDefinitions
                .stream()
                .map(StorageProviderDefinitionDTO::from)
                .toList();
    }

    @GetMapping("{key}/{version}/")
    @Operation(
            summary = "Retrieve Storage Provider Definition",
            description = "Retrieve a specific storage provider definition by its key and version. " +
                    "Requires at least one of the system-level permissions `" +
                    StoragePermissionProvider.STORAGE_PROVIDER_READ + "` or `" +
                    StoragePermissionProvider.STORAGE_PROVIDER_CREATE + "`."
    )
    public StorageProviderDefinitionDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String key,
            @Nonnull @PathVariable Integer version
    ) throws ResponseException {
        requireDefinitionAccess(jwt);

        return storageProviderDefinitions
                .stream()
                .filter(def -> def.getKey().equals(key) && def.getMajorVersion().equals(version))
                .findFirst()
                .map(StorageProviderDefinitionDTO::from)
                .orElseThrow(ResponseException::notFound);
    }

    @PostMapping("{key}/{version}/derive/")
    @Operation(
            summary = "Derive Storage Provider Configuration",
            description = "Derives configuration values against the backend-defined configuration layout of a storage provider definition. " +
                    "Requires at least one of the system-level permissions `" +
                    StoragePermissionProvider.STORAGE_PROVIDER_READ + "` or `" +
                    StoragePermissionProvider.STORAGE_PROVIDER_CREATE + "`."
    )
    public DerivedRuntimeElementData derive(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String key,
            @Nonnull @PathVariable Integer version,
            @Nonnull @Valid @RequestBody ElementValuesDerivationRequestDTO request
    ) throws ResponseException {
        requireDefinitionAccess(jwt);

        var definition = storageProviderDefinitions
                .stream()
                .filter(def -> def.getKey().equals(key) && def.getMajorVersion().equals(version))
                .findFirst()
                .orElseThrow(ResponseException::notFound);

        var layout = definition.getProviderConfigLayout();
        if (layout == null) {
            return DerivedRuntimeElementData.empty();
        }

        return elementDerivationService.derive(new ElementDerivationRequest(
                layout,
                request.authoredElementValues(),
                request.derivationOptions()
        ));
    }

    private void requireDefinitionAccess(@Nullable Jwt jwt) throws ResponseException {
        // Definition metadata is needed both for reading existing providers and for configuring a new one.
        // Do not require storage_provider.read here, otherwise create-only users cannot open the create form.
        if (
                !permissionService.hasSystemPermission(jwt, StoragePermissionProvider.STORAGE_PROVIDER_READ) &&
                        !permissionService.hasSystemPermission(jwt, StoragePermissionProvider.STORAGE_PROVIDER_CREATE)
        ) {
            throw ResponseException.forbidden(
                    "Sie benötigen die Berechtigung %s oder %s auf Systemebene.",
                    StringUtils.quote(StoragePermissionProvider.STORAGE_PROVIDER_READ),
                    StringUtils.quote(StoragePermissionProvider.STORAGE_PROVIDER_CREATE)
            );
        }
    }

    public record StorageProviderDefinitionDTO(
            @Nonnull String key,
            @Nonnull Integer version,
            @Nonnull String name,
            @Nonnull String abstractDescription,
            @Nonnull String description,
            @Nullable String documentationUrl,
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
                    definition.getAbstract(),
                    definition.getDescription(),
                    definition.getDocumentationUrl(),
                    definition.getSupportsMetadataAttributes(),
                    layout
            );
        }

    }
}
