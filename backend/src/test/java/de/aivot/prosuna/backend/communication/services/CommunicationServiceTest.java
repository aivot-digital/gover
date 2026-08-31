package de.aivot.prosuna.backend.communication.services;

import de.aivot.prosuna.backend.communication.entities.CommunicationProviderBindingEntity;
import de.aivot.prosuna.backend.communication.entities.CommunicationProviderEntity;
import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.communication.models.CommunicationProviderContext;
import de.aivot.prosuna.backend.communication.models.CommunicationProviderDefinition;
import de.aivot.prosuna.backend.communication.repositories.CommunicationProviderBindingRepository;
import de.aivot.prosuna.backend.communication.repositories.CommunicationProviderRepository;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityProviderType;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.identity.repositories.IdentityProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommunicationServiceTest {
    private final CommunicationProviderBindingRepository bindingRepository = mock(CommunicationProviderBindingRepository.class);
    private final CommunicationProviderRepository providerRepository = mock(CommunicationProviderRepository.class);
    private final IdentityProviderRepository identityProviderRepository = mock(IdentityProviderRepository.class);
    private final CommunicationProviderDefinitionService definitionService = mock(CommunicationProviderDefinitionService.class);
    private final CommunicationProviderConfigurationService configurationService = mock(CommunicationProviderConfigurationService.class);
    private final DefaultMailCommunicationService defaultMailCommunicationService = mock(DefaultMailCommunicationService.class);
    private final CommunicationProviderDefinition<String, String> definition = mock(CommunicationProviderDefinition.class);

    private CommunicationService communicationService;
    private IdentityData identity;
    private CommunicationProviderBindingEntity binding;
    private CommunicationProviderEntity provider;
    private IdentityProviderEntity identityProvider;

    @BeforeEach
    void setUp() throws Exception {
        communicationService = new CommunicationService(
                bindingRepository,
                providerRepository,
                identityProviderRepository,
                definitionService,
                configurationService,
                defaultMailCommunicationService
        );

        var identityProviderKey = UUID.randomUUID();
        identity = new IdentityData(
                "session", "applicant", IdentityType.IdentityProvider, identityProviderKey, "metadata", null,
                Map.of(), 12, Map.of()
        );
        binding = new CommunicationProviderBindingEntity()
                .setId(12)
                .setIdentityProviderKey(identityProviderKey)
                .setCommunicationProviderId(7)
                .setName("E-Mail")
                .setDescription("E-Mail")
                .setEnabled(true)
                .setPosition(0)
                .setConfiguration(new AuthoredElementValues());
        provider = new CommunicationProviderEntity();
        provider.setId(7);
        provider.setCommunicationProviderDefinitionKey("core.mail");
        provider.setCommunicationProviderDefinitionVersion(1);
        provider.setName("Mail");
        provider.setDescription("Mail");
        provider.setConfiguration(new AuthoredElementValues());
        provider.setEnabled(true);
        provider.setTestProvider(false);
        identityProvider = new IdentityProviderEntity()
                .setKey(identityProviderKey)
                .setName("BundID")
                .setDescription("BundID")
                .setType(IdentityProviderType.BundId)
                .setIsEnabled(true)
                .setIsTestProvider(false);

        when(bindingRepository.findById(12)).thenReturn(Optional.of(binding));
        when(providerRepository.findById(7)).thenReturn(Optional.of(provider));
        when(identityProviderRepository.findById(identityProviderKey)).thenReturn(Optional.of(identityProvider));
        when(definitionService.retrieveProviderDefinition("core.mail", 1)).thenReturn(Optional.of(definition));
        when(definition.supportsIdentityProvider(identityProvider)).thenReturn(true);
        when(configurationService.mapProviderConfiguration(provider, definition)).thenReturn("provider-config");
        when(configurationService.mapBindingConfiguration(binding, identityProvider, definition)).thenReturn("binding-config");
    }

    @Test
    void sendMessageResolvesSelectedBindingAndInvokesDefinition() {
        var message = new CommunicationMessage("Subject", "Body", Instant.now(), null);

        communicationService.sendMessage(identity, message);

        var contextCaptor = org.mockito.ArgumentCaptor.forClass(CommunicationProviderContext.class);
        verify(definition).sendMessage(contextCaptor.capture(), eq(identity), eq(message));
        var context = contextCaptor.getValue();
        assertSame(provider, context.communicationProvider());
        assertSame(identityProvider, context.identityProvider());
        assertSame(binding, context.binding());
        assertEquals("provider-config", context.communicationProviderConfiguration());
        assertEquals("binding-config", context.identityProviderBindingConfiguration());
    }

    @Test
    void sendMessageRejectsTestAndProductionProviderMismatch() {
        provider.setTestProvider(true);

        assertThrows(CommunicationException.class, () -> communicationService.sendMessage(
                identity,
                new CommunicationMessage("Subject", "Body", Instant.now(), null)
        ));

        verify(definition, never()).sendMessage(any(), any(), any());
    }

    @Test
    void sendMessageRequiresASelectedBinding() {
        var identityWithoutSelection = new IdentityData(
                identity.sessionId(), identity.identityId(), IdentityType.IdentityProvider, identity.providerKey(),
                identity.metadataIdentifier(), null, identity.attributes(), null, Map.of()
        );

        assertThrows(CommunicationException.class, () -> communicationService.sendMessage(
                identityWithoutSelection,
                new CommunicationMessage("Subject", "Body", Instant.now(), null)
        ));

        verify(definition, never()).sendMessage(any(), any(), any());
    }

    @Test
    void availableBindingsContainOnlyFullyUsableBindings() {
        var disabled = new CommunicationProviderBindingEntity()
                .setId(13)
                .setIdentityProviderKey(identity.providerKey())
                .setCommunicationProviderId(provider.getId())
                .setName("Disabled")
                .setDescription("Disabled")
                .setEnabled(false)
                .setPosition(1)
                .setConfiguration(new AuthoredElementValues());
        when(bindingRepository.findAllByIdentityProviderKeyOrderByPositionAscNameAscIdAsc(identity.providerKey()))
                .thenReturn(List.of(binding, disabled));

        assertEquals(List.of(binding), communicationService.getAvailableBindings(identity));
    }

    @Test
    void emailIdentityUsesDefaultMailWithoutResolvingAProvider() {
        var emailIdentity = new IdentityData(
                "session", "applicant", IdentityType.Email, null, null, "customer@example.test",
                Map.of(), null, Map.of()
        );
        var message = new CommunicationMessage("Subject", "Body", Instant.now(), null);

        communicationService.sendMessage(emailIdentity, message);

        verify(defaultMailCommunicationService).sendMessage("customer@example.test", message);
        verify(bindingRepository, never()).findById(any());
        verify(definition, never()).sendMessage(any(), any(), any());
    }
}
