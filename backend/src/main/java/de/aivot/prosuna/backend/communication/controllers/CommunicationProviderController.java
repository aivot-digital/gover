package de.aivot.prosuna.backend.communication.controllers;

import de.aivot.prosuna.backend.communication.entities.CommunicationProviderBindingEntity;
import de.aivot.prosuna.backend.communication.entities.CommunicationProviderEntity;
import de.aivot.prosuna.backend.communication.models.CommunicationProviderDefinition;
import de.aivot.prosuna.backend.communication.permissions.CommunicationProviderPermissionProvider;
import de.aivot.prosuna.backend.communication.services.CommunicationProviderDefinitionService;
import de.aivot.prosuna.backend.communication.services.CommunicationProviderManagementService;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.identity.enums.IdentityProviderType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/communication-providers/")
@Tag(name = "Communication Providers")
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class CommunicationProviderController {
    private final CommunicationProviderManagementService managementService;
    private final CommunicationProviderDefinitionService definitionService;
    private final PermissionService permissionService;

    public CommunicationProviderController(CommunicationProviderManagementService managementService,
                                           CommunicationProviderDefinitionService definitionService,
                                           PermissionService permissionService) {
        this.managementService = managementService;
        this.definitionService = definitionService;
        this.permissionService = permissionService;
    }

    @GetMapping("")
    public List<ProviderResponse> list(@Nullable @AuthenticationPrincipal Jwt jwt) throws ResponseException {
        permissionService.requireSystemPermission(jwt, CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_READ);
        return managementService.listProviders().stream().map(ProviderResponse::from).toList();
    }

    @GetMapping("{id}/")
    public ProviderResponse retrieve(@Nullable @AuthenticationPrincipal Jwt jwt,
                                     @Nonnull @PathVariable Integer id) throws ResponseException {
        permissionService.requireSystemPermission(jwt, CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_READ);
        return ProviderResponse.from(managementService.getProvider(id));
    }

    @PostMapping("")
    public ProviderResponse create(@Nullable @AuthenticationPrincipal Jwt jwt,
                                   @Nonnull @Valid @RequestBody ProviderRequest request) throws ResponseException {
        permissionService.requireSystemPermission(jwt, CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_CREATE);
        return ProviderResponse.from(managementService.createProvider(request.toEntity()));
    }

    @PutMapping("{id}/")
    public ProviderResponse update(@Nullable @AuthenticationPrincipal Jwt jwt,
                                   @Nonnull @PathVariable Integer id,
                                   @Nonnull @Valid @RequestBody ProviderRequest request) throws ResponseException {
        permissionService.requireSystemPermission(jwt, CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_UPDATE);
        return ProviderResponse.from(managementService.updateProvider(id, request.toEntity()));
    }

    @DeleteMapping("{id}/")
    public void delete(@Nullable @AuthenticationPrincipal Jwt jwt,
                       @Nonnull @PathVariable Integer id) throws ResponseException {
        permissionService.requireSystemPermission(jwt, CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_DELETE);
        managementService.deleteProvider(id);
    }

    @GetMapping("definitions/")
    public List<DefinitionResponse> definitions(@Nullable @AuthenticationPrincipal Jwt jwt) throws ResponseException {
        permissionService.requireSystemPermission(jwt, CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_READ);
        return definitionService.getAllProviderDefinitions().stream().map(DefinitionResponse::from).toList();
    }

    @GetMapping("definitions/configuration/")
    public ConfigLayoutElement providerConfigurationLayout(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestParam String definitionKey,
            @Nonnull @RequestParam Integer version
    ) throws ResponseException {
        permissionService.requireSystemPermission(jwt, CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_READ);
        return managementService.getProviderConfigurationLayout(definitionKey, version);
    }

    @GetMapping("bindings/")
    public List<BindingResponse> bindings(@Nullable @AuthenticationPrincipal Jwt jwt,
                                         @Nonnull @RequestParam UUID identityProviderKey) throws ResponseException {
        permissionService.requireSystemPermission(jwt, CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_READ);
        return managementService.listBindings(identityProviderKey).stream().map(BindingResponse::from).toList();
    }

    @GetMapping("bindings/configuration/")
    public ConfigLayoutElement bindingConfigurationLayout(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestParam Integer communicationProviderId,
            @Nonnull @RequestParam UUID identityProviderKey
    ) throws ResponseException {
        permissionService.requireSystemPermission(jwt, CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_READ);
        return managementService.getBindingConfigurationLayout(communicationProviderId, identityProviderKey);
    }

    @PostMapping("bindings/")
    public BindingResponse createBinding(@Nullable @AuthenticationPrincipal Jwt jwt,
                                         @Nonnull @Valid @RequestBody BindingRequest request) throws ResponseException {
        permissionService.requireSystemPermission(jwt, CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_CREATE);
        return BindingResponse.from(managementService.createBinding(request.toEntity()));
    }

    @PutMapping("bindings/{id}/")
    public BindingResponse updateBinding(@Nullable @AuthenticationPrincipal Jwt jwt,
                                         @Nonnull @PathVariable Integer id,
                                         @Nonnull @Valid @RequestBody BindingRequest request) throws ResponseException {
        permissionService.requireSystemPermission(jwt, CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_UPDATE);
        return BindingResponse.from(managementService.updateBinding(id, request.toEntity()));
    }

    @DeleteMapping("bindings/{id}/")
    public void deleteBinding(@Nullable @AuthenticationPrincipal Jwt jwt,
                              @Nonnull @PathVariable Integer id) throws ResponseException {
        permissionService.requireSystemPermission(jwt, CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_DELETE);
        managementService.deleteBinding(id);
    }

    public record ProviderRequest(
            @NotBlank String communicationProviderDefinitionKey,
            @NotNull Integer communicationProviderDefinitionVersion,
            @NotBlank @Size(max = 64) String name,
            @NotNull @Size(max = 255) String description,
            @NotNull AuthoredElementValues configuration,
            @NotNull Boolean isEnabled,
            @NotNull Boolean isTestProvider
    ) {
        CommunicationProviderEntity toEntity() {
            var entity = new CommunicationProviderEntity();
            entity.setCommunicationProviderDefinitionKey(communicationProviderDefinitionKey);
            entity.setCommunicationProviderDefinitionVersion(communicationProviderDefinitionVersion);
            entity.setName(name);
            entity.setDescription(description);
            entity.setConfiguration(configuration);
            entity.setEnabled(isEnabled);
            entity.setTestProvider(isTestProvider);
            return entity;
        }
    }

    public record ProviderResponse(
            Integer id,
            String communicationProviderDefinitionKey,
            Integer communicationProviderDefinitionVersion,
            String name,
            String description,
            AuthoredElementValues configuration,
            Boolean isEnabled,
            Boolean isTestProvider
    ) {
        static ProviderResponse from(CommunicationProviderEntity entity) {
            return new ProviderResponse(entity.getId(), entity.getCommunicationProviderDefinitionKey(),
                    entity.getCommunicationProviderDefinitionVersion(), entity.getName(), entity.getDescription(),
                    entity.getConfiguration(), entity.getEnabled(), entity.getTestProvider());
        }
    }

    public record BindingRequest(
            @NotNull UUID identityProviderKey,
            @NotNull Integer communicationProviderId,
            @NotBlank @Size(max = 64) String name,
            @NotNull @Size(max = 255) String description,
            @NotNull Boolean isEnabled,
            @NotNull Integer position,
            @NotNull AuthoredElementValues configuration
    ) {
        CommunicationProviderBindingEntity toEntity() {
            return new CommunicationProviderBindingEntity()
                    .setIdentityProviderKey(identityProviderKey)
                    .setCommunicationProviderId(communicationProviderId)
                    .setName(name)
                    .setDescription(description)
                    .setEnabled(isEnabled)
                    .setPosition(position)
                    .setConfiguration(configuration);
        }
    }

    public record BindingResponse(
            Integer id,
            UUID identityProviderKey,
            Integer communicationProviderId,
            String name,
            String description,
            Boolean isEnabled,
            Integer position,
            AuthoredElementValues configuration
    ) {
        static BindingResponse from(CommunicationProviderBindingEntity entity) {
            return new BindingResponse(entity.getId(), entity.getIdentityProviderKey(), entity.getCommunicationProviderId(),
                    entity.getName(), entity.getDescription(), entity.getEnabled(), entity.getPosition(), entity.getConfiguration());
        }
    }

    public record DefinitionResponse(
            String key,
            Integer version,
            String name,
            String description,
            List<IdentityProviderType> supportedIdentityProviderTypes
    ) {
        static DefinitionResponse from(CommunicationProviderDefinition<?, ?> definition) {
            return new DefinitionResponse(definition.getKey(), definition.getMajorVersion(), definition.getName(),
                    definition.getDescription(), definition.getSupportedIdentityProviderTypes());
        }
    }
}
