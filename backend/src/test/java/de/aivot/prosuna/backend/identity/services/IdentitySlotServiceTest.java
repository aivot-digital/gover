package de.aivot.prosuna.backend.identity.services;

import de.aivot.prosuna.backend.communication.entities.CommunicationProviderBindingEntity;
import de.aivot.prosuna.backend.communication.services.CommunicationService;
import de.aivot.prosuna.backend.communication.services.IdentityCommunicationService;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.form.input.IdentityConfigElementOption;
import de.aivot.prosuna.backend.elements.models.elements.form.input.IdentityConfigElementSlot;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityProviderType;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdentitySlotServiceTest {
    private static final int PROCESS_NODE_ID = 23;
    private static final String IDENTITY_ID = "representative";

    private IdentityProviderService identityProviderService;
    private IdentityCommunicationService identityCommunicationService;
    private CommunicationService communicationService;
    private IdentitySlotService service;

    @BeforeEach
    void setUp() {
        identityProviderService = mock(IdentityProviderService.class);
        identityCommunicationService = mock(IdentityCommunicationService.class);
        communicationService = mock(CommunicationService.class);
        service = new IdentitySlotService(
                mock(IdentityService.class),
                identityProviderService,
                identityCommunicationService,
                communicationService
        );
    }

    @Test
    void resolveSlot_ReturnsReadyConfiguredProviderAndCommunicationState() throws ResponseException {
        var providerKey = UUID.randomUUID();
        var iconKey = UUID.randomUUID();
        var provider = provider(providerKey, iconKey);
        var slot = new IdentityConfigElementSlot(
                IDENTITY_ID,
                "Vertretung",
                "Identität der vertretenden Person",
                false,
                false,
                List.of(new IdentityConfigElementOption(providerKey, List.of("legal-representation")))
        );
        var identities = new IdentityDataMap();
        identities.put(IDENTITY_ID, providerIdentity(providerKey));
        var communicationState = new IdentityCommunicationService.SelectionState(
                true,
                true,
                7,
                List.of(),
                null,
                new AuthoredElementValues(),
                new DerivedRuntimeElementData()
        );

        when(identityProviderService.retrieve(providerKey)).thenReturn(Optional.of(provider));
        when(communicationService.getUsableBindings(provider))
                .thenReturn(List.of(mock(CommunicationProviderBindingEntity.class)));
        when(identityCommunicationService.getState("identity-session", PROCESS_NODE_ID, IDENTITY_ID))
                .thenReturn(communicationState);

        var result = service.resolveSlot(
                slot,
                identities,
                "identity-session",
                PROCESS_NODE_ID
        );

        assertEquals(IDENTITY_ID, result.id());
        assertEquals("Vertretung", result.title());
        assertFalse(result.isOptional());
        assertFalse(result.allowsEmail());
        assertEquals(IdentityType.IdentityProvider, result.identityType());
        assertTrue(result.isReady());
        assertSame(communicationState, result.communication());
        assertEquals(1, result.availableIdentityProviders().size());
        var providerOption = result.availableIdentityProviders().getFirst();
        assertEquals(providerKey, providerOption.identityProviderKey());
        assertEquals("BundID", providerOption.identityProviderName());
        assertEquals(iconKey, providerOption.identityProviderAssetKey());
        assertEquals(IdentityProviderType.BundId, providerOption.identityProviderType());
        assertTrue(providerOption.isAuthenticatedWithThis());
        assertEquals(List.of("legal-representation"), providerOption.additionalScopes());
    }

    @Test
    void resolveSlot_TreatsAllowedEmailAsReadyWithoutCommunicationSelection() throws ResponseException {
        var slot = new IdentityConfigElementSlot(
                IDENTITY_ID,
                null,
                null,
                true,
                true,
                List.of()
        );
        var identities = new IdentityDataMap();
        identities.put(IDENTITY_ID, new IdentityData(
                "identity-session",
                IDENTITY_ID,
                IdentityType.Email,
                null,
                null,
                null,
                "person@example.test",
                Map.of(),
                null,
                Map.of()
        ));

        var result = service.resolveSlot(slot, identities, "identity-session", PROCESS_NODE_ID);

        assertTrue(result.isReady());
        assertTrue(result.isOptional());
        assertTrue(result.allowsEmail());
        assertEquals(IdentityType.Email, result.identityType());
        assertEquals("person@example.test", result.emailAddress());
        assertNull(result.communication());
        assertTrue(result.availableIdentityProviders().isEmpty());
    }

    private static IdentityProviderEntity provider(UUID providerKey, UUID iconKey) {
        return new IdentityProviderEntity()
                .setKey(providerKey)
                .setName("BundID")
                .setIconAssetKey(iconKey)
                .setType(IdentityProviderType.BundId)
                .setIsEnabled(true);
    }

    private static IdentityData providerIdentity(UUID providerKey) {
        return new IdentityData(
                "identity-session",
                IDENTITY_ID,
                IdentityType.IdentityProvider,
                providerKey,
                "bund-id",
                "provider-user-123",
                null,
                Map.of(),
                7,
                Map.of()
        );
    }
}
