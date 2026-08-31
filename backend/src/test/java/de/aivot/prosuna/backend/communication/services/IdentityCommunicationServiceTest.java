package de.aivot.prosuna.backend.communication.services;

import de.aivot.prosuna.backend.communication.entities.CommunicationProviderBindingEntity;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.identity.cache.entities.IdentityCacheEntity;
import de.aivot.prosuna.backend.identity.cache.repositories.IdentityCacheRepository;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityCommunicationServiceTest {
    private final IdentityCacheRepository cacheRepository = mock(IdentityCacheRepository.class);
    private final CommunicationService communicationService = mock(CommunicationService.class);
    private final IdentityCommunicationService service = new IdentityCommunicationService(cacheRepository, communicationService);

    private IdentityCacheEntity cacheEntity;
    private CommunicationProviderBindingEntity mail;
    private CommunicationProviderBindingEntity inbox;

    @BeforeEach
    void setUp() {
        var providerKey = UUID.randomUUID();
        cacheEntity = new IdentityCacheEntity(
                "cache", "session", 11, null, IdentityType.IdentityProvider, providerKey, "applicant", "metadata", null,
                "https://example.test", "state", Map.of("sub", "123"), null, null
        );
        mail = binding(1, providerKey, "E-Mail");
        inbox = binding(2, providerKey, "Postfach");
        when(cacheRepository.findAllBySessionIdAndRelatedProcessNodeId("session", 11))
                .thenReturn(List.of(cacheEntity));
    }

    @Test
    void noAvailableBindingIsAConfigurationConflict() {
        when(communicationService.getAvailableBindings(any())).thenReturn(List.of());

        var exception = assertThrows(
                ResponseException.class,
                () -> service.getState("session", 11, "applicant")
        );

        assertTrue(exception.getMessage().contains("keine verwendbare Kommunikationsanbindung"));
        verify(communicationService, never()).getCustomerConfiguration(any());
    }

    @Test
    void exactlyOneAvailableBindingIsSelectedAutomatically() throws Exception {
        when(communicationService.getAvailableBindings(any())).thenReturn(List.of(mail));
        when(communicationService.getCustomerConfiguration(any())).thenReturn(
                new CommunicationService.CustomerConfiguration(null, DerivedRuntimeElementData.empty(), true)
        );

        var state = service.getState("session", 11, "applicant");

        assertTrue(state.required());
        assertTrue(state.ready());
        assertEquals(mail.getId(), state.selectedBindingId());
        assertEquals(mail.getId(), cacheEntity.getCommunicationProviderBindingId());
        verify(cacheRepository).save(cacheEntity);
    }

    @Test
    void multipleAvailableBindingsRequireAnExplicitSelection() throws Exception {
        when(communicationService.getAvailableBindings(any())).thenReturn(List.of(mail, inbox));

        var state = service.getState("session", 11, "applicant");

        assertTrue(state.required());
        assertFalse(state.ready());
        assertNull(state.selectedBindingId());
        assertEquals(List.of("E-Mail", "Postfach"), state.choices().stream().map(choice -> choice.name()).toList());
        verify(cacheRepository, never()).save(any());
    }

    @Test
    void selectionPersistsBindingSpecificCustomerData() throws Exception {
        when(communicationService.getAvailableBindings(any())).thenReturn(List.of(mail, inbox));
        when(communicationService.getCustomerConfiguration(any())).thenReturn(
                new CommunicationService.CustomerConfiguration(null, DerivedRuntimeElementData.empty(), true)
        );
        var customerData = new AuthoredElementValues();
        customerData.put("email", "customer@example.test");

        var state = service.select("session", 11, "applicant", mail.getId(), customerData);

        assertEquals(mail.getId(), state.selectedBindingId());
        assertEquals("customer@example.test", cacheEntity.getCommunicationProviderData().get("email"));
        verify(cacheRepository).save(cacheEntity);
    }

    @Test
    void previewDerivesBindingSpecificCustomerDataWithoutPersistingIt() throws Exception {
        when(communicationService.getAvailableBindings(any())).thenReturn(List.of(mail, inbox));
        when(communicationService.getCustomerConfiguration(any())).thenAnswer(invocation -> {
            var identity = invocation.<de.aivot.prosuna.backend.identity.models.IdentityData>getArgument(0);
            assertEquals(mail.getId(), identity.communicationProviderBindingId());
            assertEquals("customer@example.test", identity.communicationProviderData().get("email"));
            return new CommunicationService.CustomerConfiguration(null, DerivedRuntimeElementData.empty(), true);
        });
        var customerData = new AuthoredElementValues();
        customerData.put("email", "customer@example.test");

        var state = service.preview("session", 11, "applicant", mail.getId(), customerData);

        assertTrue(state.ready());
        assertEquals(mail.getId(), state.selectedBindingId());
        assertEquals("customer@example.test", state.customerData().get("email"));
        assertNull(cacheEntity.getCommunicationProviderBindingId());
        assertNull(cacheEntity.getCommunicationProviderData());
        verify(cacheRepository, never()).save(any());
    }

    @Test
    void unavailableBindingCannotBeSelected() {
        when(communicationService.getAvailableBindings(any())).thenReturn(List.of(mail));

        assertThrows(ResponseException.class, () -> service.select(
                "session", 11, "applicant", inbox.getId(), new AuthoredElementValues()
        ));
        verify(cacheRepository, never()).save(any());
    }

    private static CommunicationProviderBindingEntity binding(int id, UUID identityProviderKey, String name) {
        return new CommunicationProviderBindingEntity()
                .setId(id)
                .setIdentityProviderKey(identityProviderKey)
                .setCommunicationProviderId(id + 10)
                .setName(name)
                .setDescription(name)
                .setEnabled(true)
                .setPosition(id)
                .setConfiguration(new AuthoredElementValues());
    }
}
