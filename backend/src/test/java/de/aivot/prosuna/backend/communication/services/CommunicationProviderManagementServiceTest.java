package de.aivot.prosuna.backend.communication.services;

import de.aivot.prosuna.backend.communication.entities.CommunicationProviderBindingEntity;
import de.aivot.prosuna.backend.communication.entities.CommunicationProviderEntity;
import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.communication.models.CommunicationProviderDefinition;
import de.aivot.prosuna.backend.communication.repositories.CommunicationProviderBindingRepository;
import de.aivot.prosuna.backend.communication.repositories.CommunicationProviderRepository;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityProviderType;
import de.aivot.prosuna.backend.identity.repositories.IdentityProviderRepository;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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

        when(providerRepository.findById(provider.getId())).thenReturn(Optional.of(provider));
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
    void invalidProviderConfigurationIsReturnedAsBadRequest() throws Exception {
        provider.setEnabled(false);
        doThrow(new CommunicationException("Die Konfiguration ist ungültig."))
                .when(configurationService)
                .mapProviderConfiguration(provider, definition);

        var exception = assertThrows(
                ResponseException.class,
                () -> service.createProvider(provider)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Die Konfiguration ist ungültig.", exception.getTitle());
        verify(providerRepository, never()).save(any());
    }

    @Test
    void mismatchedTestAndProductionProvidersCannotBeBound() {
        provider.setTestProvider(true);

        assertThrows(ResponseException.class, () -> service.createBinding(binding("Mail")));

        verify(bindingRepository, never()).saveAndFlush(any());
    }

    @Test
    void testingLayoutIsLoadedFromTheConfiguredDefinition() throws Exception {
        var layout = new GroupLayoutElement();
        when(definition.getTestingLayout()).thenReturn(layout);

        var result = service.getProviderTestingLayout(provider.getId());

        assertSame(layout, result);
        verify(definitionService).retrieveProviderDefinition(
                provider.getCommunicationProviderDefinitionKey(),
                provider.getCommunicationProviderDefinitionVersion()
        );
    }

    @Test
    void testMapsTheStoredConfigurationAndInvokesTheDefinition() throws Exception {
        var inputs = new AuthoredElementValues();
        inputs.put("recipient", "customer@example.test");
        var configuration = new Object();
        when(configurationService.mapProviderConfiguration(provider, definition)).thenReturn(configuration);

        service.testProvider(provider.getId(), inputs);

        verify(configurationService).mapProviderConfiguration(provider, definition);
        verify(definition).handleTest(provider, configuration, inputs);
    }

    @Test
    void invalidStoredConfigurationPreventsTheTest() throws Exception {
        var inputs = new AuthoredElementValues();
        doThrow(new CommunicationException("Konfiguration ungültig"))
                .when(configurationService)
                .mapProviderConfiguration(provider, definition);

        var exception = assertThrows(
                ResponseException.class,
                () -> service.testProvider(provider.getId(), inputs)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Konfiguration ungültig", exception.getTitle());
        verify(definition, never()).handleTest(any(), any(), any());
    }

    @Test
    void communicationFailureDuringTestBecomesBadRequest() throws Exception {
        var inputs = new AuthoredElementValues();
        var configuration = new Object();
        when(configurationService.mapProviderConfiguration(provider, definition)).thenReturn(configuration);
        doThrow(new CommunicationException("Versand fehlgeschlagen"))
                .when(definition)
                .handleTest(provider, configuration, inputs);

        var exception = assertThrows(
                ResponseException.class,
                () -> service.testProvider(provider.getId(), inputs)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Versand fehlgeschlagen", exception.getTitle());
    }

    @Test
    void unexpectedFailureDuringTestBecomesInternalServerError() throws Exception {
        var inputs = new AuthoredElementValues();
        var configuration = new Object();
        when(configurationService.mapProviderConfiguration(provider, definition)).thenReturn(configuration);
        doThrow(new IllegalStateException("unexpected"))
                .when(definition)
                .handleTest(provider, configuration, inputs);

        var exception = assertThrows(
                ResponseException.class,
                () -> service.testProvider(provider.getId(), inputs)
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        assertEquals("Der Kommunikationsanbieter Mail konnte nicht getestet werden.", exception.getTitle());
    }

    @Test
    void testingUnknownProviderReturnsNotFound() {
        var exception = assertThrows(
                ResponseException.class,
                () -> service.getProviderTestingLayout(999)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void testingProviderWithMissingDefinitionReturnsBadRequest() {
        when(definitionService.retrieveProviderDefinition(
                provider.getCommunicationProviderDefinitionKey(),
                provider.getCommunicationProviderDefinitionVersion()
        )).thenReturn(Optional.empty());

        var exception = assertThrows(
                ResponseException.class,
                () -> service.testProvider(provider.getId(), new AuthoredElementValues())
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Die Kommunikationsanbieter-Definition ist nicht verfügbar.", exception.getTitle());
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
