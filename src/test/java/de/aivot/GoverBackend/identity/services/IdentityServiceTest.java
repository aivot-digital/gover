package de.aivot.GoverBackend.identity.services;

import de.aivot.GoverBackend.core.exceptions.HttpConnectionException;
import de.aivot.GoverBackend.core.models.HttpServiceHeaders;
import de.aivot.GoverBackend.core.services.HttpService;
import de.aivot.GoverBackend.identity.cache.entities.IdentityCacheEntity;
import de.aivot.GoverBackend.identity.cache.repositories.IdentityCacheRepository;
import de.aivot.GoverBackend.identity.constants.IdentityQueryParameterConstants;
import de.aivot.GoverBackend.identity.entities.IdentityProviderEntity;
import de.aivot.GoverBackend.identity.models.IdentityAdditionalParameter;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.secrets.entities.SecretEntity;
import de.aivot.GoverBackend.secrets.services.SecretService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityServiceTest {
    private static final String VALID_HOSTNAME = "https://example.com";
    private static final String VALID_ORIGIN = "https://example.com/origin";
    private static final String VALID_STATE = "state-nonce";

    private GoverConfig goverConfig;
    private IdentityProviderService identityProviderService;
    private IdentityCacheRepository identityCacheRepository;
    private IdentityService identityService;
    private HttpService httpService;
    private SecretService secretService;

    @BeforeEach
    void setUp() {
        goverConfig = mock(GoverConfig.class);
        identityProviderService = mock(IdentityProviderService.class);
        identityCacheRepository = mock(IdentityCacheRepository.class);
        httpService = mock(HttpService.class);
        secretService = mock(SecretService.class);
        identityService = new IdentityService(
                goverConfig,
                secretService,
                httpService,
                identityProviderService,
                identityCacheRepository
        );
    }

    @Test
    void createRedirectURL_ShouldConstructValidURL() throws ResponseException {
        UUID providerKey = UUID.randomUUID();
        List<String> additionalScopes = List.of("scope3");

        IdentityProviderEntity provider = new IdentityProviderEntity();
        provider.setKey(providerKey);
        provider.setMetadataIdentifier("meta");
        provider.setClientId("client-id");
        provider.setAuthorizationEndpoint("https://auth.example.com/authorize");
        provider.setDefaultScopes(List.of("scope1", "scope2"));
        provider.setIsEnabled(true);
        provider.setAdditionalParams(List.of(
                new IdentityAdditionalParameter()
                        .setKey("param1")
                        .setValue("value1"),
                new IdentityAdditionalParameter()
                        .setKey("param2")
                        .setValue("value2")
        ));

        when(identityProviderService.retrieve(providerKey)).thenReturn(Optional.of(provider));
        when(goverConfig.getGoverHostname()).thenReturn(VALID_HOSTNAME);
        when(identityCacheRepository.save(any(IdentityCacheEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        URI result = identityService.createRedirectURL(providerKey, VALID_ORIGIN, additionalScopes);

        var savedIdentityCaptor = ArgumentCaptor.forClass(IdentityCacheEntity.class);
        verify(identityCacheRepository).save(savedIdentityCaptor.capture());
        var savedIdentity = savedIdentityCaptor.getValue();

        var queryParams = UriComponentsBuilder.fromUri(result).build().getQueryParams();
        var returnedState = queryParams.getFirst(IdentityQueryParameterConstants.AUTH_ENDPOINT_STATE);

        assertTrue(result.toString().contains("scope=scope1%20scope2%20scope3"));
        assertEquals("value1", queryParams.getFirst("param1"));
        assertEquals("value2", queryParams.getFirst("param2"));
        assertEquals(savedIdentity.getStateNonce(), returnedState);
        assertEquals(VALID_ORIGIN, savedIdentity.getOrigin());
        assertNotEquals(VALID_ORIGIN, returnedState);
    }

    @Test
    void createRedirectURL_ShouldThrowException_WhenProviderKeyIsNull() {
        ResponseException exception = assertThrows(ResponseException.class, () ->
                identityService.createRedirectURL(null, VALID_HOSTNAME, null)
        );

        assertEquals("Der Nutzerkontenanbieter ist nicht angegeben.", exception.getMessage());
    }

    @Test
    void createRedirectURL_ShouldThrowException_WhenOriginIsInvalid() throws ResponseException {
        UUID providerKey = UUID.randomUUID();
        String invalidOrigin = "invalid-origin";

        IdentityProviderEntity provider = new IdentityProviderEntity();
        provider.setKey(providerKey);
        provider.setMetadataIdentifier("meta");
        provider.setIsEnabled(true);

        when(identityProviderService.retrieve(providerKey)).thenReturn(Optional.of(provider));
        when(goverConfig.getGoverHostname()).thenReturn(VALID_HOSTNAME);

        ResponseException exception = assertThrows(ResponseException.class, () ->
                identityService.createRedirectURL(providerKey, invalidOrigin, null)
        );

        assertEquals("Der Referer-Header ist ungültig.", exception.getMessage());
    }

    @Test
    void createRedirectURL_ShouldCombineScopesCorrectly() throws ResponseException {
        UUID providerKey = UUID.randomUUID();
        List<String> additionalScopes = List.of("scope2", "scope3");

        IdentityProviderEntity provider = new IdentityProviderEntity();
        provider.setKey(providerKey);
        provider.setMetadataIdentifier("meta");
        provider.setClientId("client-id");
        provider.setAuthorizationEndpoint("https://auth.example.com/authorize");
        provider.setDefaultScopes(List.of("scope1", "scope2"));
        provider.setIsEnabled(true);
        provider.setAdditionalParams(List.of(
                new IdentityAdditionalParameter()
                        .setKey("param1")
                        .setValue("value1"),
                new IdentityAdditionalParameter()
                        .setKey("param2")
                        .setValue("value2")
        ));

        when(identityProviderService.retrieve(providerKey)).thenReturn(Optional.of(provider));
        when(goverConfig.getGoverHostname()).thenReturn(VALID_HOSTNAME);
        when(identityCacheRepository.save(any(IdentityCacheEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        URI result = identityService.createRedirectURL(providerKey, VALID_HOSTNAME, additionalScopes);

        assertTrue(result.toString().contains("scope=scope1%20scope2%20scope3"));
    }

    @Test
    void handleCallback_ShouldThrowException_WhenAuthorizationCodeIsNull() {
        UUID providerKey = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(identityCacheRepository.findById(sessionId))
                .thenReturn(Optional.of(createIdentityCacheEntity(sessionId, providerKey, VALID_ORIGIN, VALID_STATE)));

        ResponseException exception = assertThrows(ResponseException.class, () ->
                identityService.handleCallback(providerKey, sessionId, null, VALID_STATE)
        );

        assertEquals("Es wurde kein Autorisierungscode übergeben.", exception.getMessage());
    }

    @Test
    void handleCallback_ShouldThrowException_WhenProviderKeyIsInvalid() throws ResponseException {
        UUID providerKey = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(identityProviderService.retrieve(providerKey)).thenReturn(Optional.empty());
        when(identityCacheRepository.findById(sessionId))
                .thenReturn(Optional.of(createIdentityCacheEntity(sessionId, providerKey, VALID_ORIGIN, VALID_STATE)));

        ResponseException exception = assertThrows(ResponseException.class, () ->
                identityService.handleCallback(providerKey, sessionId, "auth-code", VALID_STATE)
        );

        assertEquals("Der Nutzerkontenanbieter existiert nicht.", exception.getMessage());
    }

    @Test
    void handleCallback_ShouldThrowException_WhenSessionNotFound() {
        UUID providerKey = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        ResponseException exception = assertThrows(ResponseException.class, () ->
                identityService.handleCallback(providerKey, sessionId, "auth-code", VALID_STATE)
        );

        assertEquals("Die Identitätssitzung existiert nicht.", exception.getMessage());
    }

    @Test
    void createRedirectURL_ShouldThrowException_WhenOriginDoesNotMatchGoverHostname() throws ResponseException {
        UUID providerKey = UUID.randomUUID();

        IdentityProviderEntity provider = new IdentityProviderEntity();
        provider.setKey(providerKey);
        provider.setMetadataIdentifier("meta");
        provider.setIsEnabled(true);

        when(identityProviderService.retrieve(providerKey)).thenReturn(Optional.of(provider));
        when(goverConfig.getGoverHostname()).thenReturn("https://other.example.com");

        ResponseException exception = assertThrows(ResponseException.class, () ->
                identityService.createRedirectURL(providerKey, VALID_HOSTNAME, null)
        );

        assertEquals("Der Referer-Header ist ungültig oder nicht erlaubt.", exception.getMessage());
    }

    @Test
    void handleCallback_ShouldProcessCallbackSuccessfully() throws Exception {
        UUID providerKey = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        IdentityProviderEntity provider = new IdentityProviderEntity();
        provider.setKey(providerKey);
        provider.setMetadataIdentifier("meta");
        provider.setIsEnabled(true);
        provider.setTokenEndpoint("https://auth.example.com/token");
        provider.setUserinfoEndpoint("https://auth.example.com/userinfo");
        provider.setAttributes(List.of());

        IdentityCacheEntity identity = createIdentityCacheEntity(sessionId, providerKey, VALID_ORIGIN, VALID_STATE);

        when(identityProviderService.retrieve(providerKey)).thenReturn(Optional.of(provider));
        when(identityCacheRepository.findById(sessionId)).thenReturn(Optional.of(identity));

        var mockTokenResponse = mockHttpResponse(200, """
                {"access_token": "access-token", "refresh_token": "refresh-token", "expires_in": 3600}
                """);
        when(httpService.postFormUrlEncoded(any(URI.class), anyMap())).thenReturn(mockTokenResponse);

        var mockUserInfoResponse = mockHttpResponse(200, """
                {"name": "John Doe", "email": "john.doe@example.com"}
                """);
        when(httpService.get(any(URI.class), any(HttpServiceHeaders.class))).thenReturn(mockUserInfoResponse);
        when(identityCacheRepository.save(any(IdentityCacheEntity.class))).thenReturn(identity);

        String result = identityService.handleCallback(providerKey, sessionId, "auth-code", VALID_STATE);

        assertNotNull(result);
        assertTrue(result.contains(sessionId.toString()));
        assertTrue(result.contains("identity-state=0"));
        assertTrue(result.startsWith(VALID_ORIGIN));
    }

    @Test
    void handleCallback_ShouldThrowException_WhenTokenRetrievalFails() throws ResponseException, HttpConnectionException {
        UUID providerKey = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        IdentityProviderEntity provider = new IdentityProviderEntity();
        provider.setKey(providerKey);
        provider.setMetadataIdentifier("meta");
        provider.setIsEnabled(true);
        provider.setTokenEndpoint("https://auth.example.com/token");
        provider.setAttributes(List.of());

        when(identityProviderService.retrieve(providerKey)).thenReturn(Optional.of(provider));
        when(identityCacheRepository.findById(sessionId))
                .thenReturn(Optional.of(createIdentityCacheEntity(sessionId, providerKey, VALID_ORIGIN, VALID_STATE)));

        var mockResponse = mockHttpResponse(400, "Bad Request");
        when(httpService.postFormUrlEncoded(
                eq(URI.create("https://auth.example.com/token")),
                anyMap()
        )).thenReturn(mockResponse);

        ResponseException exception = assertThrows(ResponseException.class, () ->
                identityService.handleCallback(providerKey, sessionId, "auth-code", VALID_STATE)
        );

        assertEquals("Ungültiger Status-Code beim Abrufen des Zugriffsschlüssels für Nutzerkontenanbieter null (" + providerKey + "): 400", exception.getMessage());
    }

    @Test
    void handleCallback_ShouldPerformLogoutSuccessfully() throws ResponseException, HttpConnectionException {
        UUID providerKey = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        IdentityProviderEntity provider = new IdentityProviderEntity();
        provider.setKey(providerKey);
        provider.setMetadataIdentifier("meta");
        provider.setIsEnabled(true);
        provider.setTokenEndpoint("https://auth.example.com/token");
        provider.setUserinfoEndpoint("https://auth.example.com/userinfo");
        provider.setEndSessionEndpoint("https://auth.example.com/logout");
        provider.setAttributes(List.of());

        when(identityProviderService.retrieve(providerKey)).thenReturn(Optional.of(provider));

        IdentityCacheEntity identity = createIdentityCacheEntity(sessionId, providerKey, VALID_ORIGIN, VALID_STATE);
        when(identityCacheRepository.findById(sessionId)).thenReturn(Optional.of(identity));

        var mockTokenResponse = mockHttpResponse(200, """
                {"access_token": "access-token", "refresh_token": "refresh-token", "expires_in": 3600}
                """);
        when(httpService.postFormUrlEncoded(
                eq(URI.create("https://auth.example.com/token")),
                anyMap()
        )).thenReturn(mockTokenResponse);

        var mockUserInfoResponse = mockHttpResponse(200, """
                {"name": "John Doe", "email": "john.doe@example.com"}
                """);
        when(httpService.get(
                eq(URI.create("https://auth.example.com/userinfo")),
                any(HttpServiceHeaders.class)
        )).thenReturn(mockUserInfoResponse);

        var mockLogoutResponse = mockHttpResponse(204, "");
        when(httpService.postFormUrlEncoded(
                eq(URI.create("https://auth.example.com/logout")),
                anyMap(),
                any(HttpServiceHeaders.class)
        )).thenReturn(mockLogoutResponse);

        when(identityCacheRepository.save(any(IdentityCacheEntity.class))).thenReturn(identity);

        String result = identityService.handleCallback(providerKey, sessionId, "auth-code", VALID_STATE);

        assertNotNull(result);
        String expectedUrl = UriComponentsBuilder
                .fromUriString(VALID_ORIGIN)
                .queryParam("identity-state", "0")
                .queryParam("identity-id", identity.getSessionId())
                .build()
                .toString();
        assertEquals(expectedUrl, result);
        verify(httpService).postFormUrlEncoded(
                eq(URI.create("https://auth.example.com/logout")),
                anyMap(),
                any(HttpServiceHeaders.class)
        );
    }

    @Test
    void handleCallback_ShouldRetrieveAndUseClientSecret() throws Exception {
        UUID providerKey = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        IdentityProviderEntity provider = new IdentityProviderEntity();
        provider.setKey(providerKey);
        provider.setMetadataIdentifier("meta");
        provider.setIsEnabled(true);
        provider.setTokenEndpoint("https://auth.example.com/token");
        provider.setUserinfoEndpoint("https://auth.example.com/userinfo");
        provider.setClientSecretKey(UUID.randomUUID());
        provider.setAttributes(List.of());

        String decryptedSecret = "decrypted-client-secret";
        when(identityProviderService.retrieve(providerKey)).thenReturn(Optional.of(provider));

        var dummySecret = new SecretEntity();
        when(secretService.retrieve(provider.getClientSecretKey())).thenReturn(Optional.of(dummySecret));
        when(secretService.decrypt(dummySecret)).thenReturn(decryptedSecret);

        IdentityCacheEntity identity = createIdentityCacheEntity(sessionId, providerKey, VALID_ORIGIN, VALID_STATE);
        when(identityCacheRepository.findById(sessionId)).thenReturn(Optional.of(identity));

        var mockTokenResponse = mockHttpResponse(200, """
                {"access_token": "access-token", "refresh_token": "refresh-token", "expires_in": 3600}
                """);
        when(httpService.postFormUrlEncoded(
                eq(URI.create("https://auth.example.com/token")),
                argThat(body -> decryptedSecret.equals(body.get("client_secret")))
        )).thenReturn(mockTokenResponse);

        var mockUserInfoResponse = mockHttpResponse(200, """
                {"name": "John Doe", "email": "john.doe@example.com"}
                """);
        when(httpService.get(
                eq(URI.create("https://auth.example.com/userinfo")),
                any(HttpServiceHeaders.class)
        )).thenReturn(mockUserInfoResponse);

        when(identityCacheRepository.save(any(IdentityCacheEntity.class))).thenReturn(identity);

        String result = identityService.handleCallback(providerKey, sessionId, "auth-code", VALID_STATE);

        assertNotNull(result);
        String expectedUrl = UriComponentsBuilder
                .fromUriString(VALID_ORIGIN)
                .queryParam("identity-state", "0")
                .queryParam("identity-id", identity.getSessionId())
                .build()
                .toString();
        assertEquals(expectedUrl, result);
        verify(secretService).retrieve(provider.getClientSecretKey());
        verify(secretService).decrypt(dummySecret);
    }

    @Test
    void handleCallback_ShouldThrowException_WhenStateDoesNotMatch() {
        UUID providerKey = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(identityCacheRepository.findById(sessionId))
                .thenReturn(Optional.of(createIdentityCacheEntity(sessionId, providerKey, VALID_ORIGIN, "different-state")));

        ResponseException exception = assertThrows(ResponseException.class, () ->
                identityService.handleCallback(providerKey, sessionId, "auth-code", VALID_STATE)
        );

        assertEquals("Der state-Parameter ist ungültig.", exception.getMessage());
    }

    @Test
    void handleCallback_ShouldThrowException_WhenCachedStateNonceIsMissing() {
        UUID providerKey = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(identityCacheRepository.findById(sessionId))
                .thenReturn(Optional.of(createIdentityCacheEntity(sessionId, providerKey, VALID_ORIGIN, "")));

        ResponseException exception = assertThrows(ResponseException.class, () ->
                identityService.handleCallback(providerKey, sessionId, "auth-code", VALID_STATE)
        );

        assertEquals("Für die Identitätssitzung " + sessionId + " wurde kein state-Nonce gespeichert.", exception.getMessage());
    }

    @Test
    void createErrorRedirectURL_ShouldUseCachedOrigin() throws ResponseException {
        UUID providerKey = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(identityCacheRepository.findById(sessionId))
                .thenReturn(Optional.of(createIdentityCacheEntity(sessionId, providerKey, VALID_ORIGIN, VALID_STATE)));

        String result = identityService.createErrorRedirectURL(
                sessionId,
                VALID_STATE,
                "access_denied",
                "The user denied access."
        );

        String expectedUrl = UriComponentsBuilder
                .fromUriString(VALID_ORIGIN)
                .queryParam("error", "access_denied")
                .queryParam("error_description", "The user denied access.")
                .queryParam("identity-state", "500")
                .build()
                .toString();
        assertEquals(expectedUrl, result);
    }

    @Test
    void createErrorRedirectURL_ShouldThrowException_WhenCachedOriginIsMissing() {
        UUID providerKey = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(identityCacheRepository.findById(sessionId))
                .thenReturn(Optional.of(createIdentityCacheEntity(sessionId, providerKey, "", VALID_STATE)));

        ResponseException exception = assertThrows(ResponseException.class, () ->
                identityService.createErrorRedirectURL(sessionId, VALID_STATE, "access_denied", null)
        );

        assertEquals("Für die Identitätssitzung " + sessionId + " wurde keine Ursprungs-URL gespeichert.", exception.getMessage());
    }

    private IdentityCacheEntity createIdentityCacheEntity(
            UUID sessionId,
            UUID providerKey,
            String origin,
            String stateNonce
    ) {
        return new IdentityCacheEntity(
                sessionId,
                null,
                providerKey.toString(),
                "meta",
                origin,
                stateNonce,
                null
        );
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> mockHttpResponse(int statusCode, String body) {
        var response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(body);
        return response;
    }
}
