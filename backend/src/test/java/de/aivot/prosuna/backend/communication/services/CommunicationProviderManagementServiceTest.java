package de.aivot.prosuna.backend.communication.services;

import de.aivot.prosuna.backend.communication.entities.CommunicationProviderBindingEntity;
import de.aivot.prosuna.backend.communication.entities.CommunicationProviderEntity;
import de.aivot.prosuna.backend.communication.models.CommunicationProviderDefinition;
import de.aivot.prosuna.backend.communication.repositories.CommunicationProviderBindingRepository;
import de.aivot.prosuna.backend.communication.repositories.CommunicationProviderRepository;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityProviderType;
import de.aivot.prosuna.backend.identity.repositories.IdentityProviderRepository;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommunicationProviderManagementServiceTest {
    private final CommunicationProviderRepository providerRepository = mock(CommunicationProviderRepository.class);
    private final CommunicationProviderBindingRepository bindingRepository = mock(CommunicationProviderBindingRepository.class);
    private final CommunicationProviderDefinitionService definitionService = mock(CommunicationProviderDefinitionService.class);
    private final CommunicationProviderConfigurationService configurationService = mock(CommunicationProviderConfigurationService.class);
    private final IdentityProviderRepository identityProviderRepository = mock(IdentityProviderRepository.class);
    private final CommunicationProviderDefinition<Object, Object> definition = mock(CommunicationProviderDefinition.class);

    private CommunicationProviderManagementService service;
    private CommunicationProviderEntity provider;
    private IdentityProviderEntity identityProvider;

    @BeforeEach
    void setUp() {
        service = new CommunicationProviderManagementService(
                providerRepository,
                bindingRepository,
                definitionService,
                configurationService,
                identityProviderRepository
        );
        provider = provider(7, true, false);
        identityProvider = identityProvider(true, false);

        when(providerRepository.findByIdForUpdate(provider.getId())).thenReturn(Optional.of(provider));
        when(identityProviderRepository.findByKeyForUpdate(identityProvider.getKey()))
                .thenReturn(Optional.of(identityProvider));
        when(definitionService.retrieveProviderDefinition(
                provider.getCommunicationProviderDefinitionKey(),
                provider.getCommunicationProviderDefinitionVersion()
        )).thenReturn(Optional.of(definition));
        when(definition.supportsIdentityProvider(identityProvider)).thenReturn(true);
        when(bindingRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(providerRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void sameCommunicationProviderCanBeAddedToAnIdentityProviderMultipleTimes() throws Exception {
        service.createBinding(binding("Primary"));
        service.createBinding(binding("Secondary"));

        verify(bindingRepository, times(2)).saveAndFlush(any(CommunicationProviderBindingEntity.class));
    }

    @Test
    void disablingTheLastUsableBindingOfAnEnabledIdentityProviderIsAllowed() {
        var existing = binding("Mail").setId(12);
        var update = binding("Mail").setId(12).setEnabled(false);
        when(bindingRepository.findById(12)).thenReturn(Optional.of(existing));
        when(bindingRepository.findByIdForUpdate(12)).thenReturn(Optional.of(existing));
        assertDoesNotThrow(() -> service.updateBinding(12, update));
        verify(bindingRepository).saveAndFlush(existing);
    }

    @Test
    void disablingAProviderThatWouldOrphanAnEnabledIdentityProviderIsAllowed() {
        var binding = binding("Mail").setId(12);
        var update = provider(7, false, false);
        when(bindingRepository.findAllByCommunicationProviderId(7)).thenReturn(List.of(binding));
        assertDoesNotThrow(() -> service.updateProvider(7, update));
        verify(providerRepository).saveAndFlush(provider);
    }

    @Test
    void mismatchedTestAndProductionProvidersCannotBeBound() {
        provider.setTestProvider(true);

        assertThrows(ResponseException.class, () -> service.createBinding(binding("Mail")));

        verify(bindingRepository, never()).saveAndFlush(any());
    }

    private CommunicationProviderBindingEntity binding(String name) {
        return new CommunicationProviderBindingEntity()
                .setIdentityProviderKey(identityProvider.getKey())
                .setCommunicationProviderId(provider.getId())
                .setName(name)
                .setDescription(name)
                .setEnabled(true)
                .setPosition(0)
                .setConfiguration(new AuthoredElementValues());
    }

    private static CommunicationProviderEntity provider(int id, boolean enabled, boolean testProvider) {
        var entity = new CommunicationProviderEntity();
        entity.setId(id);
        entity.setCommunicationProviderDefinitionKey("de.aivot.core.mail_communication_provider");
        entity.setCommunicationProviderDefinitionVersion(1);
        entity.setName("Mail");
        entity.setDescription("Mail");
        entity.setConfiguration(new AuthoredElementValues());
        entity.setEnabled(enabled);
        entity.setTestProvider(testProvider);
        return entity;
    }

    private static IdentityProviderEntity identityProvider(boolean enabled, boolean testProvider) {
        return new IdentityProviderEntity()
                .setKey(UUID.randomUUID())
                .setType(IdentityProviderType.BundId)
                .setName("BundID")
                .setDescription("BundID")
                .setIsEnabled(enabled)
                .setIsTestProvider(testProvider);
    }
}
