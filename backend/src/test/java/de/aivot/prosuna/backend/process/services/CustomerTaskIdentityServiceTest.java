package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.identity.services.IdentityService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition.CustomerView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CustomerTaskIdentityServiceTest {
    private static final String REQUIRED_IDENTITY_ID = "applicant";
    private static final int PROCESS_NODE_ID = 23;

    private IdentityService identityService;
    private CustomerTaskIdentityService service;
    private ProcessNodeEntity processNode;

    @BeforeEach
    void setUp() {
        identityService = mock(IdentityService.class);
        service = new CustomerTaskIdentityService(identityService);
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
        var requiredIdentity = providerIdentity(REQUIRED_IDENTITY_ID, "provider-user-123");
        var storedIdentities = new IdentityDataMap();
        storedIdentities.put(REQUIRED_IDENTITY_ID, requiredIdentity);
        var authenticatedIdentities = new IdentityDataMap();
        authenticatedIdentities.put("another-slot", providerIdentity("another-slot", "provider-user-123"));
        when(identityService.getIdentityDataMap("current-session", PROCESS_NODE_ID))
                .thenReturn(authenticatedIdentities);

        service.requireAuthenticatedIdentity(
                processInstance(storedIdentities),
                processNode,
                customerView(REQUIRED_IDENTITY_ID),
                "current-session"
        );

        verify(identityService).getIdentityDataMap("current-session", PROCESS_NODE_ID);
    }

    @Test
    void requireAuthenticatedIdentity_RejectsMissingOrDifferentProviderIdentity() {
        var storedIdentities = new IdentityDataMap();
        storedIdentities.put(REQUIRED_IDENTITY_ID, providerIdentity(REQUIRED_IDENTITY_ID, "provider-user-123"));
        var authenticatedIdentities = new IdentityDataMap();
        authenticatedIdentities.put(REQUIRED_IDENTITY_ID, providerIdentity(REQUIRED_IDENTITY_ID, "provider-user-456"));
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

    private static CustomerView customerView(String requiredIdentityId) {
        return new CustomerView(
                new GroupLayoutElement(),
                List.of(),
                new AuthoredElementValues(),
                requiredIdentityId
        );
    }

    private static ProcessInstanceEntity processInstance(IdentityDataMap identities) {
        return new ProcessInstanceEntity().setIdentities(identities);
    }

    private static IdentityData providerIdentity(String identityId, String uniqueId) {
        return providerIdentity(identityId, UUID.randomUUID(), uniqueId);
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
}
