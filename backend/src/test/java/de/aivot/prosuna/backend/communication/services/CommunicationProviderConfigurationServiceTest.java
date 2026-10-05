package de.aivot.prosuna.backend.communication.services;

import de.aivot.prosuna.backend.communication.entities.CommunicationProviderBindingEntity;
import de.aivot.prosuna.backend.communication.entities.CommunicationProviderEntity;
import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.communication.models.CommunicationProviderDefinition;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.ElementDerivationRequest;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.services.ElementDerivationService;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommunicationProviderConfigurationServiceTest {
    private final ElementDerivationService elementDerivationService = mock(ElementDerivationService.class);
    private final CommunicationProviderDefinition<ConfigurationWithoutDefaultConstructor, ConfigurationWithoutDefaultConstructor> definition = mock(CommunicationProviderDefinition.class);
    private final ConfigLayoutElement layout = new ConfigLayoutElement();
    private final IdentityProviderEntity identityProvider = new IdentityProviderEntity();

    private CommunicationProviderConfigurationService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new CommunicationProviderConfigurationService(elementDerivationService);
        when(definition.getConfigLayout()).thenReturn(layout);
        when(definition.getConfigClass()).thenReturn(ConfigurationWithoutDefaultConstructor.class);
        when(definition.getIdentityProviderBindingConfigLayout(identityProvider)).thenReturn(layout);
        when(definition.getIdentityProviderBindingConfigClass()).thenReturn(ConfigurationWithoutDefaultConstructor.class);
    }

    @Test
    void invalidNewProviderConfigurationKeepsValidationMessageWithoutNullId() {
        when(elementDerivationService.derive(any(ElementDerivationRequest.class)))
                .thenReturn(new DerivedRuntimeElementData().putError("required-field", "Dieses Feld ist erforderlich."));

        var exception = assertThrows(
                CommunicationException.class,
                () -> service.mapProviderConfiguration(provider(null), definition)
        );

        assertEquals(
                "Die Konfiguration des Kommunikationsanbieters Test provider ist ungültig.",
                exception.getMessage()
        );
        assertNull(exception.getCause());
    }

    @Test
    void invalidExistingProviderConfigurationIncludesItsId() {
        when(elementDerivationService.derive(any(ElementDerivationRequest.class)))
                .thenReturn(new DerivedRuntimeElementData().putError("required-field", "Dieses Feld ist erforderlich."));

        var exception = assertThrows(
                CommunicationException.class,
                () -> service.mapProviderConfiguration(provider(7), definition)
        );

        assertEquals(
                "Die Konfiguration des Kommunikationsanbieters Test provider (ID 7) ist ungültig.",
                exception.getMessage()
        );
    }

    @Test
    void unexpectedProviderMappingFailureRemainsALoadErrorWithoutNullId() {
        when(elementDerivationService.derive(any(ElementDerivationRequest.class)))
                .thenReturn(DerivedRuntimeElementData.empty());

        var exception = assertThrows(
                CommunicationException.class,
                () -> service.mapProviderConfiguration(provider(null), definition)
        );

        assertEquals(
                "Die Konfiguration des Kommunikationsanbieters Test provider konnte nicht geladen werden.",
                exception.getMessage()
        );
        assertNotNull(exception.getCause());
    }

    @Test
    void invalidNewBindingConfigurationKeepsValidationMessageWithoutNullId() {
        when(elementDerivationService.derive(any(ElementDerivationRequest.class)))
                .thenReturn(new DerivedRuntimeElementData().putError("required-field", "Dieses Feld ist erforderlich."));

        var exception = assertThrows(
                CommunicationException.class,
                () -> service.mapBindingConfiguration(binding(null), identityProvider, definition)
        );

        assertEquals(
                "Die Konfiguration der Kommunikationsanbindung Test binding ist ungültig.",
                exception.getMessage()
        );
        assertNull(exception.getCause());
    }

    private static CommunicationProviderEntity provider(Integer id) {
        var provider = new CommunicationProviderEntity();
        provider.setName("Test provider");
        provider.setConfiguration(new AuthoredElementValues());
        if (id != null) {
            provider.setId(id);
        }
        return provider;
    }

    private static CommunicationProviderBindingEntity binding(Integer id) {
        var binding = new CommunicationProviderBindingEntity();
        binding.setName("Test binding");
        binding.setConfiguration(new AuthoredElementValues());
        if (id != null) {
            binding.setId(id);
        }
        return binding;
    }

    private static class ConfigurationWithoutDefaultConstructor {
        private ConfigurationWithoutDefaultConstructor(String value) {
        }
    }
}
