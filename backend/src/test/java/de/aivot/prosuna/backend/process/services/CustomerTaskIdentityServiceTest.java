package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.form.input.IdentityConfigElementSlot;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.identity.dtos.IdentitySlotResponseDTO;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityProviderType;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.identity.services.IdentityProviderService;
import de.aivot.prosuna.backend.identity.services.IdentityService;
import de.aivot.prosuna.backend.identity.services.IdentitySlotService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.models.ProcessNodeCustomerView;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultInstanceCompleted;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskUpdated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CustomerTaskIdentityServiceTest {
    private static final String REQUIRED_IDENTITY_ID = "applicant";
    private static final int PROCESS_NODE_ID = 23;

    private IdentityService identityService;
    private IdentityProviderService identityProviderService;
    private IdentitySlotService identitySlotService;
    private CustomerTaskIdentityService service;
    private ProcessNodeEntity processNode;

    @BeforeEach
    void setUp() {
        identityService = mock(IdentityService.class);
        identityProviderService = mock(IdentityProviderService.class);
        identitySlotService = mock(IdentitySlotService.class);
        service = new CustomerTaskIdentityService(identityService, identityProviderService, identitySlotService);
        processNode = new ProcessNodeEntity().setId(PROCESS_NODE_ID);
    }

    @Test
    void requireAuthenticatedIdentity_AllowsViewWithoutRequirement() throws ResponseException {
        service.requireAuthenticatedIdentity(
                processInstance(new IdentityDataMap()),
                processNode,
                customerView(null),
                null
        );
        service.requireAuthenticatedIdentity(
                processInstance(new IdentityDataMap()),
                processNode,
                customerView("  "),
                null
        );

        verifyNoInteractions(identityService);
    }

    @Test
    void requireAuthenticatedIdentity_AllowsEmailIdentityWithoutAuthentication() throws ResponseException {
        var identities = new IdentityDataMap();
        identities.put(REQUIRED_IDENTITY_ID, emailIdentity(REQUIRED_IDENTITY_ID));

        service.requireAuthenticatedIdentity(
                processInstance(identities),
                processNode,
                customerView(REQUIRED_IDENTITY_ID),
                null
        );

        verifyNoInteractions(identityService);
    }

    @Test
    void requireAuthenticatedIdentity_AllowsMatchingProviderIdentityByUniqueProviderId() throws ResponseException {
        var providerKey = UUID.randomUUID();
        var requiredIdentity = providerIdentity(REQUIRED_IDENTITY_ID, providerKey, "provider-user-123");
        var storedIdentities = new IdentityDataMap();
        storedIdentities.put(REQUIRED_IDENTITY_ID, requiredIdentity);
        var authenticatedIdentities = new IdentityDataMap();
        authenticatedIdentities.put("another-slot", providerIdentity("another-slot", providerKey, "provider-user-123"));
        configureProvider(providerKey);
        when(identityService.getIdentityDataMap("current-session", PROCESS_NODE_ID))
                .thenReturn(authenticatedIdentities);

        var state = service.requireAuthenticatedIdentity(
                processInstance(storedIdentities),
                processNode,
                customerView(REQUIRED_IDENTITY_ID),
                "current-session"
        );

        assertTrue(state.isReady());
        assertEquals(REQUIRED_IDENTITY_ID, state.existingIdentity().id());
        assertTrue(state.existingIdentity().isReady());
        assertEquals(providerKey, state.existingIdentity().identityProvider().identityProviderKey());
        assertEquals("BundID", state.existingIdentity().identityProvider().identityProviderName());
        assertTrue(state.existingIdentity().identityProvider().isAuthenticatedWithThis());
        verify(identityService).getIdentityDataMap("current-session", PROCESS_NODE_ID);
    }

    @Test
    void requireAuthenticatedIdentity_RejectsSameProviderUserIdFromDifferentProvider() {
        var requiredProviderKey = UUID.randomUUID();
        var otherProviderKey = UUID.randomUUID();
        var storedIdentities = new IdentityDataMap();
        storedIdentities.put(
                REQUIRED_IDENTITY_ID,
                providerIdentity(REQUIRED_IDENTITY_ID, requiredProviderKey, "provider-user-123")
        );
        var authenticatedIdentities = new IdentityDataMap();
        authenticatedIdentities.put(
                "other-slot",
                providerIdentity("other-slot", otherProviderKey, "provider-user-123")
        );
        configureProvider(requiredProviderKey);
        when(identityService.getIdentityDataMap("current-session", PROCESS_NODE_ID))
                .thenReturn(authenticatedIdentities);

        var exception = assertThrows(ResponseException.class, () -> service.requireAuthenticatedIdentity(
                processInstance(storedIdentities),
                processNode,
                customerView(REQUIRED_IDENTITY_ID),
                "current-session"
        ));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void requireAuthenticatedIdentity_RejectsMissingOrDifferentProviderIdentity() {
        var providerKey = UUID.randomUUID();
        var storedIdentities = new IdentityDataMap();
        storedIdentities.put(REQUIRED_IDENTITY_ID, providerIdentity(REQUIRED_IDENTITY_ID, providerKey, "provider-user-123"));
        var authenticatedIdentities = new IdentityDataMap();
        authenticatedIdentities.put(REQUIRED_IDENTITY_ID, providerIdentity(REQUIRED_IDENTITY_ID, providerKey, "provider-user-456"));
        configureProvider(providerKey);
        when(identityService.getIdentityDataMap("current-session", PROCESS_NODE_ID))
                .thenReturn(authenticatedIdentities);

        var exception = assertThrows(ResponseException.class, () -> service.requireAuthenticatedIdentity(
                processInstance(storedIdentities),
                processNode,
                customerView(REQUIRED_IDENTITY_ID),
                "current-session"
        ));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals(
                CustomerTaskIdentityService.REQUIRED_IDENTITY_AUTHENTICATION_REASON,
                ((Map<?, ?>) exception.getDetails()).get("reason")
        );
    }

    @Test
    void requireAuthenticatedIdentity_FailsWhenRequiredIdentityDoesNotExistInProcessInstance() {
        var exception = assertThrows(ResponseException.class, () -> service.requireAuthenticatedIdentity(
                processInstance(new IdentityDataMap()),
                processNode,
                customerView(REQUIRED_IDENTITY_ID),
                null
        ));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        verifyNoInteractions(identityService);
    }

    @Test
    void createAuthenticationRedirect_UsesRequiredIdentityProviderAndCurrentTaskNode() throws ResponseException {
        var providerKey = UUID.randomUUID();
        var requiredIdentity = providerIdentity(REQUIRED_IDENTITY_ID, providerKey, "provider-user-123");
        var identities = new IdentityDataMap();
        identities.put(REQUIRED_IDENTITY_ID, requiredIdentity);
        configureProvider(providerKey);
        var expectedRedirect = URI.create("https://identity.example.test/login");
        when(identityService.createRedirectURL(
                "current-session",
                providerKey,
                REQUIRED_IDENTITY_ID,
                "https://prosuna.example.test/process/instance/tasks/task",
                List.of(),
                PROCESS_NODE_ID
        )).thenReturn(expectedRedirect);

        var redirect = service.createAuthenticationRedirect(
                processInstance(identities),
                processNode,
                customerView(REQUIRED_IDENTITY_ID),
                "current-session",
                "https://prosuna.example.test/process/instance/tasks/task"
        );

        assertSame(expectedRedirect, redirect);
        verify(identityService).createRedirectURL(
                "current-session",
                providerKey,
                REQUIRED_IDENTITY_ID,
                "https://prosuna.example.test/process/instance/tasks/task",
                List.of(),
                PROCESS_NODE_ID
        );
    }

    @Test
    void createAuthenticationRedirect_RejectsEmailIdentity() throws ResponseException {
        var identities = new IdentityDataMap();
        identities.put(REQUIRED_IDENTITY_ID, emailIdentity(REQUIRED_IDENTITY_ID));

        var exception = assertThrows(ResponseException.class, () -> service.createAuthenticationRedirect(
                processInstance(identities),
                processNode,
                customerView(REQUIRED_IDENTITY_ID),
                null,
                "https://prosuna.example.test/process/instance/tasks/task"
        ));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(identityService, never()).createRedirectURL(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void resolveIdentityState_AllowsEmptyOptionalNewIdentitySlot() throws ResponseException {
        var slot = new IdentityConfigElementSlot().setId("representative").setIsOptional(true);
        var identities = new IdentityDataMap();
        var slotResponse = identitySlot("representative", true, null, false);
        when(identitySlotService.requireConfiguredIdentityId(slot)).thenReturn("representative");
        when(identityService.getIdentityDataMap("current-session", PROCESS_NODE_ID)).thenReturn(identities);
        when(identitySlotService.resolveSlot(slot, identities, "current-session", PROCESS_NODE_ID))
                .thenReturn(slotResponse);

        var state = service.resolveIdentityState(
                processInstance(new IdentityDataMap()),
                processNode,
                customerViewWithNewIdentity(slot),
                "current-session"
        );

        assertTrue(state.isReady());
        assertSame(slotResponse, state.newIdentitySlot());
        assertNull(state.newIdentity());
    }

    @Test
    void resolveIdentityState_BlocksIncompleteSelectionInOptionalNewIdentitySlot() throws ResponseException {
        var slot = new IdentityConfigElementSlot().setId("representative").setIsOptional(true);
        var identities = new IdentityDataMap();
        var slotResponse = identitySlot("representative", true, IdentityType.IdentityProvider, false);
        when(identitySlotService.requireConfiguredIdentityId(slot)).thenReturn("representative");
        when(identityService.getIdentityDataMap("current-session", PROCESS_NODE_ID)).thenReturn(identities);
        when(identitySlotService.resolveSlot(slot, identities, "current-session", PROCESS_NODE_ID))
                .thenReturn(slotResponse);

        var state = service.resolveIdentityState(
                processInstance(new IdentityDataMap()),
                processNode,
                customerViewWithNewIdentity(slot),
                "current-session"
        );

        assertFalse(state.isReady());
        assertThrows(ResponseException.class, () -> service.requireAuthenticatedIdentity(state));
    }

    @Test
    void resolveIdentityState_RejectsNewIdentityIdAlreadyPresentInProcessInstance() throws ResponseException {
        var slot = new IdentityConfigElementSlot().setId("representative");
        var processIdentities = new IdentityDataMap();
        processIdentities.put("representative", emailIdentity("representative"));
        when(identitySlotService.requireConfiguredIdentityId(slot)).thenReturn("representative");

        var exception = assertThrows(ResponseException.class, () -> service.resolveIdentityState(
                processInstance(processIdentities),
                processNode,
                customerViewWithNewIdentity(slot),
                "current-session"
        ));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        verifyNoInteractions(identityService);
    }

    @Test
    void getAdditionalIdentitiesForCompletion_ReturnsReadyNewIdentityOnlyForCompletion() throws ResponseException {
        var slot = new IdentityConfigElementSlot().setId("representative");
        var identity = emailIdentity("representative");
        var cachedIdentities = new IdentityDataMap();
        cachedIdentities.put(identity.identityId(), identity);
        when(identitySlotService.requireConfiguredIdentityId(slot)).thenReturn(identity.identityId());
        when(identityService.getIdentityDataMap("current-session", PROCESS_NODE_ID)).thenReturn(cachedIdentities);
        when(identitySlotService.resolveSlot(slot, cachedIdentities, "current-session", PROCESS_NODE_ID))
                .thenReturn(identitySlot(identity.identityId(), false, IdentityType.Email, true));
        var state = service.resolveIdentityState(
                processInstance(new IdentityDataMap()),
                processNode,
                customerViewWithNewIdentity(slot),
                "current-session"
        );

        assertTrue(service.getAdditionalIdentitiesForCompletion(
                state,
                new ProcessNodeExecutionResultTaskUpdated()
        ).isEmpty());
        assertEquals(
                Map.of(identity.identityId(), identity),
                service.getAdditionalIdentitiesForCompletion(
                        state,
                        new ProcessNodeExecutionResultInstanceCompleted()
                )
        );
    }

    private static ProcessNodeCustomerView customerView(String requiredIdentityId) {
        return new ProcessNodeCustomerView(
                new GroupLayoutElement(),
                List.of(),
                new AuthoredElementValues(),
                requiredIdentityId
        );
    }

    private static ProcessNodeCustomerView customerViewWithNewIdentity(IdentityConfigElementSlot newIdentitySlot) {
        return new ProcessNodeCustomerView(
                new GroupLayoutElement(),
                List.of(),
                new AuthoredElementValues(),
                null,
                newIdentitySlot
        );
    }

    private static IdentitySlotResponseDTO identitySlot(String identityId,
                                                        boolean optional,
                                                        IdentityType identityType,
                                                        boolean ready) {
        return new IdentitySlotResponseDTO(
                identityId,
                null,
                null,
                optional,
                false,
                identityType,
                null,
                ready,
                List.of(),
                null
        );
    }

    private static ProcessInstanceEntity processInstance(IdentityDataMap identities) {
        return new ProcessInstanceEntity().setIdentities(identities);
    }

    private static IdentityData providerIdentity(String identityId, UUID providerKey, String uniqueId) {
        return new IdentityData(
                "stored-session",
                identityId,
                IdentityType.IdentityProvider,
                providerKey,
                "metadata",
                uniqueId,
                null,
                Map.of(),
                null,
                Map.of()
        );
    }

    private static IdentityData emailIdentity(String identityId) {
        return new IdentityData(
                "stored-session",
                identityId,
                IdentityType.Email,
                null,
                null,
                null,
                "customer@example.test",
                Map.of(),
                null,
                Map.of()
        );
    }

    private void configureProvider(UUID providerKey) {
        var provider = new IdentityProviderEntity()
                .setKey(providerKey)
                .setName("BundID")
                .setType(IdentityProviderType.BundId)
                .setIsEnabled(true);
        try {
            when(identityProviderService.retrieve(providerKey)).thenReturn(Optional.of(provider));
        } catch (ResponseException e) {
            throw new AssertionError(e);
        }
    }
}
