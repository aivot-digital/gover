package de.aivot.prosuna.backend.communication.services;

import de.aivot.prosuna.backend.communication.entities.CommunicationProviderBindingEntity;
import de.aivot.prosuna.backend.communication.entities.CommunicationProviderEntity;
import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.communication.models.CommunicationProviderDefinition;
import de.aivot.prosuna.backend.communication.repositories.CommunicationProviderBindingRepository;
import de.aivot.prosuna.backend.communication.repositories.CommunicationProviderRepository;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import de.aivot.prosuna.backend.identity.repositories.IdentityProviderRepository;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import jakarta.annotation.Nonnull;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class CommunicationProviderManagementService {
    private final CommunicationProviderRepository providerRepository;
    private final CommunicationProviderBindingRepository bindingRepository;
    private final CommunicationProviderDefinitionService definitionService;
    private final CommunicationProviderConfigurationService configurationService;
    private final IdentityProviderRepository identityProviderRepository;

    public CommunicationProviderManagementService(CommunicationProviderRepository providerRepository,
                                                  CommunicationProviderBindingRepository bindingRepository,
                                                  CommunicationProviderDefinitionService definitionService,
                                                  CommunicationProviderConfigurationService configurationService,
                                                  IdentityProviderRepository identityProviderRepository) {
        this.providerRepository = providerRepository;
        this.bindingRepository = bindingRepository;
        this.definitionService = definitionService;
        this.configurationService = configurationService;
        this.identityProviderRepository = identityProviderRepository;
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
        var bindings = bindingRepository.findAllByCommunicationProviderId(id);
        if (!Objects.equals(existing.getCommunicationProviderDefinitionKey(), update.getCommunicationProviderDefinitionKey())
                || !Objects.equals(existing.getCommunicationProviderDefinitionVersion(), update.getCommunicationProviderDefinitionVersion())) {
            throw ResponseException.badRequest("Definition und Version eines Kommunikationsanbieters können nach der Erstellung nicht geändert werden.");
        }
        if (!Objects.equals(existing.getTestProvider(), update.getTestProvider())
                && !bindings.isEmpty()) {
            throw ResponseException.conflict(
                    "Die Umgebung eines Kommunikationsanbieters kann nicht geändert werden, solange Anbindungen bestehen."
            );
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
            throw ResponseException.conflict("Der Kommunikationsanbieter ist noch mit mindestens einem Nutzerkontenanbieter verbunden.");
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
        var identityProvider = getIdentityProviderForUpdate(binding.getIdentityProviderKey());
        validateBinding(binding, provider, identityProvider);
        return bindingRepository.saveAndFlush(binding);
    }

    @Nonnull
    @Transactional(rollbackFor = ResponseException.class)
    public CommunicationProviderBindingEntity updateBinding(@Nonnull Integer id,
                                                            @Nonnull CommunicationProviderBindingEntity update) throws ResponseException {
        var reference = bindingRepository.findById(id).orElseThrow(ResponseException::notFound);
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
        existing.setPosition(update.getPosition());
        existing.setConfiguration(update.getConfiguration());
        validateBinding(existing, provider, identityProvider);
        return bindingRepository.saveAndFlush(existing);
    }

    @Transactional(rollbackFor = ResponseException.class)
    public void deleteBinding(@Nonnull Integer id) throws ResponseException {
        var reference = bindingRepository.findById(id).orElseThrow(ResponseException::notFound);
        getProviderForUpdate(reference.getCommunicationProviderId());
        var identityProvider = getIdentityProviderForUpdate(reference.getIdentityProviderKey());
        var binding = bindingRepository.findByIdForUpdate(id).orElseThrow(ResponseException::notFound);
        bindingRepository.delete(binding);
        bindingRepository.flush();
    }

    @Nonnull
    public ConfigLayoutElement getProviderConfigurationLayout(@Nonnull String definitionKey,
                                                              @Nonnull Integer version) throws ResponseException {
        return getDefinition(definitionKey, version).getConfigLayout();
    }

    @Nonnull
    public ConfigLayoutElement getBindingConfigurationLayout(@Nonnull Integer providerId,
                                                             @Nonnull UUID identityProviderKey) throws ResponseException {
        var provider = getProvider(providerId);
        var identityProvider = getIdentityProvider(identityProviderKey);
        var definition = getDefinition(provider.getCommunicationProviderDefinitionKey(), provider.getCommunicationProviderDefinitionVersion());
        if (!definition.supportsIdentityProvider(identityProvider)) {
            throw ResponseException.badRequest("Der Kommunikationsanbieter unterstützt diesen Nutzerkontenanbieter nicht.");
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
            throw ResponseException.badRequest("Der Kommunikationsanbieter unterstützt diesen Nutzerkontenanbieter nicht.");
        }
        if (!Objects.equals(provider.getTestProvider(), identityProvider.getIsTestProvider())) {
            throw ResponseException.badRequest("Test- und Produktivanbieter dürfen nicht miteinander verbunden werden.");
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
    ) {
        configurationService.mapProviderConfiguration(provider, definition);
    }

    private <I> void validateBindingConfigurationTyped(
            @Nonnull CommunicationProviderBindingEntity binding,
            @Nonnull IdentityProviderEntity identityProvider,
            @Nonnull CommunicationProviderDefinition<?, I> definition
    ) {
        configurationService.mapBindingConfiguration(binding, identityProvider, definition);
    }
}
