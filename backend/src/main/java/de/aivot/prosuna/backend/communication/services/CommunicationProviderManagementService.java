package de.aivot.prosuna.backend.communication.services;

import de.aivot.prosuna.backend.audit.enums.AuditAction;
import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.audit.services.ScopedAuditService;
import de.aivot.prosuna.backend.communication.permissions.CommunicationProviderPermissionProvider;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.user.services.UserService;
import org.springframework.security.oauth2.jwt.Jwt;
import de.aivot.prosuna.backend.communication.entities.CommunicationProviderBindingEntity;
import de.aivot.prosuna.backend.communication.entities.CommunicationProviderEntity;
import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.communication.models.CommunicationProviderDefinition;
import de.aivot.prosuna.backend.communication.repositories.CommunicationProviderBindingRepository;
import de.aivot.prosuna.backend.communication.repositories.CommunicationProviderRepository;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.form.content.AlertContentElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.enums.AlertType;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import de.aivot.prosuna.backend.identity.repositories.IdentityProviderRepository;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Objects;
import java.util.UUID;

@Service
public class CommunicationProviderManagementService {
    private final CommunicationProviderRepository providerRepository;
    private final CommunicationProviderBindingRepository bindingRepository;
    private final CommunicationProviderDefinitionService definitionService;
    private final CommunicationProviderConfigurationService configurationService;
    private final IdentityProviderRepository identityProviderRepository;
    private final PermissionService permissionService;
    private final ScopedAuditService auditService;

    public CommunicationProviderManagementService(CommunicationProviderRepository providerRepository,
                                                  CommunicationProviderBindingRepository bindingRepository,
                                                  CommunicationProviderDefinitionService definitionService,
                                                  CommunicationProviderConfigurationService configurationService,
                                                  IdentityProviderRepository identityProviderRepository,
                                                  PermissionService permissionService,
                                                  AuditService auditService) {
        this.providerRepository = providerRepository;
        this.bindingRepository = bindingRepository;
        this.definitionService = definitionService;
        this.configurationService = configurationService;
        this.identityProviderRepository = identityProviderRepository;
        this.permissionService = permissionService;
        this.auditService = auditService.createScopedAuditService(CommunicationProviderManagementService.class, "Kommunikationsanbindungen");
    }

    @Nonnull
    public List<CommunicationProviderEntity> listProviders() {
        return providerRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Nonnull
    public CommunicationProviderEntity getProvider(@Nonnull Integer id) throws ResponseException {
        return providerRepository.findById(id).orElseThrow(ResponseException::notFound);
    }

    @Nonnull
    public CommunicationProviderEntity createProvider(@Nonnull CommunicationProviderEntity entity) throws ResponseException {
        validateProvider(entity);
        return providerRepository.save(entity);
    }

    @Nonnull
    @Transactional(rollbackFor = ResponseException.class)
    public CommunicationProviderEntity updateProvider(@Nonnull Integer id,
                                                       @Nonnull CommunicationProviderEntity update) throws ResponseException {
        var existing = providerRepository.findByIdForUpdate(id).orElseThrow(ResponseException::notFound);
        if (!Objects.equals(existing.getCommunicationProviderDefinitionKey(), update.getCommunicationProviderDefinitionKey())
                || !Objects.equals(existing.getCommunicationProviderDefinitionVersion(), update.getCommunicationProviderDefinitionVersion())) {
            throw ResponseException.badRequest("Definition und Version eines Kommunikationsanbieters können nach der Erstellung nicht geändert werden.");
        }

        existing.setName(update.getName());
        existing.setDescription(update.getDescription());
        existing.setConfiguration(update.getConfiguration());
        existing.setEnabled(update.getEnabled());
        existing.setTestProvider(update.getTestProvider());
        validateProvider(existing);
        var saved = providerRepository.saveAndFlush(existing);
        return saved;
    }

    @Transactional(rollbackFor = ResponseException.class)
    public void deleteProvider(@Nonnull Integer id) throws ResponseException {
        var provider = providerRepository.findByIdForUpdate(id).orElseThrow(ResponseException::notFound);
        if (provider.getEnabled()) {
            throw ResponseException.conflict("Der Kommunikationsanbieter muss vor dem Löschen deaktiviert werden.");
        }
        if (!bindingRepository.findAllByCommunicationProviderId(id).isEmpty()) {
            throw ResponseException.conflict("Der Kommunikationsanbieter ist noch mit mindestens einem Identitätsanbieter verbunden.");
        }
        providerRepository.delete(provider);
    }

    @Nonnull
    public List<CommunicationProviderBindingEntity> listBindings(@Nonnull UUID identityProviderKey) throws ResponseException {
        getIdentityProvider(identityProviderKey);
        return bindingRepository.findAllByIdentityProviderKeyOrderByPositionAscNameAscIdAsc(identityProviderKey);
    }

    @Nonnull
    @Transactional(rollbackFor = ResponseException.class)
    public CommunicationProviderBindingEntity createBinding(@Nonnull CommunicationProviderBindingEntity binding) throws ResponseException {
        var provider = getProviderForUpdate(binding.getCommunicationProviderId());
        if (!provider.getEnabled()) {
            throw ResponseException.badRequest("Wählen Sie einen aktiven Kommunikationsanbieter aus.");
        }
        var identityProvider = getIdentityProviderForUpdate(binding.getIdentityProviderKey());
        validateBinding(binding, provider, identityProvider);
        var existing = bindingRepository.findAllByIdentityProviderKeyOrderByPositionAscNameAscIdAsc(binding.getIdentityProviderKey());
        // The identity-provider lock serializes appends and reorders, including inactive bindings.
        for (var position = 0; position < existing.size(); position++) {
            existing.get(position).setPosition(position);
        }
        bindingRepository.saveAllAndFlush(existing);
        binding.setPosition(existing.size());
        return bindingRepository.saveAndFlush(binding);
    }

    @Nonnull
    @Transactional(rollbackFor = ResponseException.class)
    public CommunicationProviderBindingEntity updateBinding(@Nonnull Integer id,
                                                            @Nonnull CommunicationProviderBindingEntity update) throws ResponseException {
        var reference = bindingRepository.findReferenceById(id).orElseThrow(ResponseException::notFound);
        var provider = getProviderForUpdate(reference.getCommunicationProviderId());
        var identityProvider = getIdentityProviderForUpdate(reference.getIdentityProviderKey());
        var existing = bindingRepository.findByIdForUpdate(id).orElseThrow(ResponseException::notFound);
        if (!Objects.equals(existing.getIdentityProviderKey(), update.getIdentityProviderKey())
                || !Objects.equals(existing.getCommunicationProviderId(), update.getCommunicationProviderId())) {
            throw ResponseException.badRequest("Nutzerkonto- und Kommunikationsanbieter einer Anbindung können nach der Erstellung nicht geändert werden.");
        }
        existing.setName(update.getName());
        existing.setDescription(update.getDescription());
        existing.setEnabled(update.getEnabled());
        existing.setConfiguration(update.getConfiguration());
        validateBinding(existing, provider, identityProvider);
        return bindingRepository.saveAndFlush(existing);
    }

    @Transactional(rollbackFor = ResponseException.class)
    public void deleteBinding(@Nonnull Integer id) throws ResponseException {
        var reference = bindingRepository.findReferenceById(id).orElseThrow(ResponseException::notFound);
        getProviderForUpdate(reference.getCommunicationProviderId());
        var identityProvider = getIdentityProviderForUpdate(reference.getIdentityProviderKey());
        var binding = bindingRepository.findByIdForUpdate(id).orElseThrow(ResponseException::notFound);
        bindingRepository.delete(binding);
        bindingRepository.flush();
    }

    @Nonnull
    @Transactional(rollbackFor = ResponseException.class)
    public List<CommunicationProviderBindingEntity> reorderBindings(@Nullable Jwt jwt,
                                                                    @Nonnull UUID identityProviderKey,
                                                                    @Nonnull List<Integer> ids) throws ResponseException {
        permissionService.requireSystemPermission(jwt, CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_UPDATE);
        getIdentityProviderForUpdate(identityProviderKey);
        var bindings = bindingRepository.findAllByIdentityProviderKeyOrderByPositionAscNameAscIdAsc(identityProviderKey);
        var idSet = new HashSet<>(ids);
        // A complete permutation prevents cross-provider IDs and stale lists from dropping bindings.
        if (idSet.size() != ids.size() || ids.size() != bindings.size()
                || bindings.stream().anyMatch(binding -> !idSet.contains(binding.getId()))) {
            throw ResponseException.conflict("Die Kommunikationsanbindungen haben sich geändert oder die Reihenfolge ist ungültig. Laden Sie die Liste erneut.");
        }
        var oldOrder = bindings.stream().map(CommunicationProviderBindingEntity::getId).toList();
        var byId = bindings.stream().collect(Collectors.toMap(CommunicationProviderBindingEntity::getId, binding -> binding));
        for (var position = 0; position < ids.size(); position++) {
            byId.get(ids.get(position)).setPosition(position);
        }
        var ordered = ids.stream().map(byId::get).toList();
        bindingRepository.saveAllAndFlush(ordered);
        auditService.create()
                .setActorId(UserService.getIdFromJWT(jwt))
                .withAuditAction(AuditAction.Update, IdentityProviderEntity.class, identityProviderKey, "key")
                .withDiff(Map.of("communicationBindingIds", oldOrder), Map.of("communicationBindingIds", ids))
                .withMessage("Die Reihenfolge der Kommunikationsanbindungen wurde geändert.")
                .log();
        return ordered;
    }

    @Nonnull
    public ConfigLayoutElement getProviderConfigurationLayout(@Nonnull String definitionKey,
                                                              @Nonnull Integer version) throws ResponseException {
        return getDefinition(definitionKey, version).getConfigLayout();
    }

    @Nullable
    public GroupLayoutElement getProviderTestingLayout(@Nonnull Integer providerId) throws ResponseException {
        var provider = getProvider(providerId);
        var definition = getDefinition(
                provider.getCommunicationProviderDefinitionKey(),
                provider.getCommunicationProviderDefinitionVersion()
        );
        return definition.getTestingLayout();
    }

    @Nonnull
    public GroupLayoutElement testProvider(@Nonnull Integer providerId,
                                           @Nonnull AuthoredElementValues inputs) throws ResponseException {
        var provider = getProvider(providerId);
        var definition = getDefinition(
                provider.getCommunicationProviderDefinitionKey(),
                provider.getCommunicationProviderDefinitionVersion()
        );
        try {
            return testProviderTyped(provider, definition, inputs);
        } catch (CommunicationException e) {
            return createFailedTestResult(e.getMessage());
        } catch (RuntimeException e) {
            throw ResponseException.internalServerError(
                    "Der Kommunikationsanbieter %s konnte nicht getestet werden.".formatted(provider.getName()),
                    e
            );
        }
    }

    @Nonnull
    public ConfigLayoutElement getBindingConfigurationLayout(@Nonnull Integer providerId,
                                                             @Nonnull UUID identityProviderKey) throws ResponseException {
        var provider = getProvider(providerId);
        var identityProvider = getIdentityProvider(identityProviderKey);
        var definition = getDefinition(provider.getCommunicationProviderDefinitionKey(), provider.getCommunicationProviderDefinitionVersion());
        if (!definition.supportsIdentityProvider(identityProvider)) {
            throw ResponseException.badRequest("Der Kommunikationsanbieter unterstützt diesen Identitätsanbieter nicht.");
        }
        return definition.getIdentityProviderBindingConfigLayout(identityProvider);
    }

    private void validateProvider(@Nonnull CommunicationProviderEntity provider) throws ResponseException {
        var definition = getDefinition(
                provider.getCommunicationProviderDefinitionKey(),
                provider.getCommunicationProviderDefinitionVersion()
        );
        if (provider.getEnabled() && definition.getSupportedIdentityProviderTypes().isEmpty()) {
            throw ResponseException.badRequest("Diese Kommunikationsanbieter-Definition ist noch nicht aktivierbar.");
        }
        try {
            validateProviderConfigurationTyped(provider, definition);
        } catch (CommunicationException e) {
            throw ResponseException.badRequest(e.getMessage());
        }
    }

    private void validateBinding(@Nonnull CommunicationProviderBindingEntity binding,
                                 @Nonnull CommunicationProviderEntity provider,
                                 @Nonnull IdentityProviderEntity identityProvider) throws ResponseException {
        var definition = getDefinition(
                provider.getCommunicationProviderDefinitionKey(),
                provider.getCommunicationProviderDefinitionVersion()
        );
        if (!definition.supportsIdentityProvider(identityProvider)) {
            throw ResponseException.badRequest("Der Kommunikationsanbieter unterstützt diesen Identitätsanbieter nicht.");
        }
        try {
            validateBindingConfigurationTyped(binding, identityProvider, definition);
        } catch (CommunicationException e) {
            throw ResponseException.badRequest(e.getMessage());
        }
    }

    @Nonnull
    private CommunicationProviderDefinition<?, ?> getDefinition(@Nonnull String key,
                                                                 @Nonnull Integer version) throws ResponseException {
        return definitionService.retrieveProviderDefinition(key, version)
                .orElseThrow(() -> ResponseException.badRequest("Die Kommunikationsanbieter-Definition ist nicht verfügbar."));
    }

    @Nonnull
    private IdentityProviderEntity getIdentityProvider(@Nonnull UUID key) throws ResponseException {
        return identityProviderRepository.findById(key).orElseThrow(ResponseException::notFound);
    }

    @Nonnull
    private CommunicationProviderEntity getProviderForUpdate(@Nonnull Integer id) throws ResponseException {
        return providerRepository.findByIdForUpdate(id).orElseThrow(ResponseException::notFound);
    }

    @Nonnull
    private IdentityProviderEntity getIdentityProviderForUpdate(@Nonnull UUID key) throws ResponseException {
        return identityProviderRepository.findByKeyForUpdate(key).orElseThrow(ResponseException::notFound);
    }

    private <C> void validateProviderConfigurationTyped(
            @Nonnull CommunicationProviderEntity provider,
            @Nonnull CommunicationProviderDefinition<C, ?> definition
    ) throws CommunicationException {
        configurationService.mapProviderConfiguration(provider, definition);
    }

    @Nonnull
    private <C> GroupLayoutElement testProviderTyped(
            @Nonnull CommunicationProviderEntity provider,
            @Nonnull CommunicationProviderDefinition<C, ?> definition,
            @Nonnull AuthoredElementValues inputs
    ) throws CommunicationException {
        var configuration = configurationService.mapProviderConfiguration(provider, definition);
        return definition.handleTest(provider, configuration, inputs);
    }

    @Nonnull
    private static GroupLayoutElement createFailedTestResult(@Nullable String message) {
        var alert = new AlertContentElement();
        alert.setId("communication-provider-test-result-alert");
        alert.setAlertType(AlertType.Error);
        alert.setTitle("Test fehlgeschlagen");
        alert.setText(message);

        var layout = new GroupLayoutElement();
        layout.setId("communication-provider-test-result");
        layout.setChildren(List.of(alert));
        return layout;
    }

    private <I> void validateBindingConfigurationTyped(
            @Nonnull CommunicationProviderBindingEntity binding,
            @Nonnull IdentityProviderEntity identityProvider,
            @Nonnull CommunicationProviderDefinition<?, I> definition
    ) throws CommunicationException {
        configurationService.mapBindingConfiguration(binding, identityProvider, definition);
    }
}
