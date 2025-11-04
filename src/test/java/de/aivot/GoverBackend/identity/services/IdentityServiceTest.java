package de.aivot.GoverBackend.identity.services;

import de.aivot.GoverBackend.core.exceptions.HttpConnectionException;
import de.aivot.GoverBackend.core.models.HttpServiceHeaders;
import de.aivot.GoverBackend.core.services.HttpService;
import de.aivot.GoverBackend.identity.cache.entities.IdentityCacheEntity;
import de.aivot.GoverBackend.identity.cache.repositories.IdentityCacheRepository;
import de.aivot.GoverBackend.identity.entities.IdentityProviderEntity;
import de.aivot.GoverBackend.identity.models.IdentityAdditionalParameter;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.secrets.entities.SecretEntity;
import de.aivot.GoverBackend.secrets.services.SecretService;
import de.aivot.GoverBackend.system.properties.CORSProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IdentityServiceTest {
    private GoverConfig goverConfig;
    private IdentityProviderService identityProviderService;
    private IdentityCacheRepository identityCacheRepository;
    private IdentityService identityService;
    private HttpService httpService;
    private SecretService secretService;
    private CORSProperties corsProperties;

    @BeforeEach
    void setUp() {
        goverConfig = mock(GoverConfig.class);
        identityProviderService = mock(IdentityProviderService.class);
        identityCacheRepository = mock(IdentityCacheRepository.class);
        httpService = mock(HttpService.class);
        secretService = mock(SecretService.class);
        corsProperties = mock(CORSProperties.class);
        identityService = new IdentityService(
                goverConfig,
                corsProperties,
                secretService,
                httpService,
                identityProviderService,
                identityCacheRepository
        );
    }

    @Test
    void createRedirectURL_ShouldConstructValidURL() throws ResponseException {
        UUID providerKey = UUID.randomUUID();
        String origin = "https://example.com";
        List<String> additionalScopes = List.of("scope3");

        IdentityProviderEntity provider = new IdentityProviderEntity();
        provider.setKey(providerKey);
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
        when(goverConfig.getGoverHostname()).thenReturn("https://example.com");
        // Mock save to return a known sessionId
        UUID sessionId = UUID.randomUUID();
        IdentityCacheEntity savedEntity = new IdentityCacheEntity(sessionId, null, providerKey.toString(), "meta", null);
        when(identityCacheRepository.save(any(IdentityCacheEntity.class))).thenReturn(savedEntity);

        URI result = identityService.createRedirectURL(providerKey, origin, additionalScopes);

        // The redirect_uri should contain the sessionId
        assertTrue(result.toString().contains("scope=scope1%20scope2%20scope3"));
        assertTrue(result.toString().contains("param1=value1"));
        assertTrue(result.toString().contains("param2=value2"));
    }

    @Test
    void createRedirectURL_ShouldThrowException_WhenProviderKeyIsNull() {
        String origin = "https://example.com";

        ResponseException exception = assertThrows(ResponseException.class, () ->
                identityService.createRedirectURL(null, origin, null)
        );

        assertEquals("Der Nutzerkontenanbieter ist nicht angegeben.", exception.getMessage());
    }

    @Test
    void createRedirectURL_ShouldThrowException_WhenOriginIsInvalid() throws ResponseException {
        UUID providerKey = UUID.randomUUID();
        String invalidOrigin = "invalid-origin";

        IdentityProviderEntity provider = new IdentityProviderEntity();
        provider.setKey(providerKey);
        provider.setIsEnabled(true);

        when(identityProviderService.retrieve(providerKey)).thenReturn(Optional.of(provider));
        when(goverConfig.getGoverHostname()).thenReturn("https://example.com");

        ResponseException exception = assertThrows(ResponseException.class, () ->
                identityService.createRedirectURL(providerKey, invalidOrigin, null)
        );

        assertEquals("Der Referer-Header ist ungültig.", exception.getMessage());
    }

    @Test
    void createRedirectURL_ShouldCombineScopesCorrectly() throws ResponseException {
        UUID providerKey = UUID.randomUUID();
        String origin = "https://example.com";
        List<String> additionalScopes = List.of("scope2", "scope3");

        IdentityProviderEntity provider = new IdentityProviderEntity();
        provider.setKey(providerKey);
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
        when(goverConfig.getGoverHostname()).thenReturn("https://example.com");
        UUID sessionId = UUID.randomUUID();
        IdentityCacheEntity savedEntity = new IdentityCacheEntity(sessionId, null, providerKey.toString(), "meta", null);
        when(identityCacheRepository.save(any(IdentityCacheEntity.class))).thenReturn(savedEntity);

        URI result = identityService.createRedirectURL(providerKey, origin, additionalScopes);

        assertTrue(result.toString().contains("scope=scope1%20scope2%20scope3"));
    }

    @Test
    void handleCallback_ShouldThrowException_WhenAuthorizationCodeIsNull() {
        UUID providerKey = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        String origin = "https://example.com/origin";
        ResponseException exception = assertThrows(ResponseException.class, () ->
                identityService.handleCallback(providerKey, sessionId, null, origin)
        );
        assertEquals("Es wurde kein Autorisierungscode übergeben.", exception.getMessage());
    }

    @Test
    void handleCallback_ShouldThrowException_WhenProviderKeyIsInvalid() throws ResponseException {
        UUID providerKey = UUID.randomUUID();
        String authorizationCode = "auth-code";
        UUID sessionId = UUID.randomUUID();
        String origin = "https://example.com/origin";
        when(identityProviderService.retrieve(providerKey)).thenReturn(Optional.empty());
        when(identityCacheRepository.findById(sessionId)).thenReturn(Optional.of(new IdentityCacheEntity(sessionId, null, providerKey.toString(), "meta", null)));
        ResponseException exception = assertThrows(ResponseException.class, () ->
                identityService.handleCallback(providerKey, sessionId, authorizationCode, origin)
        );
        assertEquals("Der Nutzerkontenanbieter existiert nicht.", exception.getMessage());
    }

    @Test
    void handleCallback_ShouldThrowException_WhenSessionNotFound() throws ResponseException {
        UUID providerKey = UUID.randomUUID();
        String authorizationCode = "auth-code";
        UUID sessionId = UUID.randomUUID();
        String origin = "https://example.com/origin";
        when(identityProviderService.retrieve(providerKey)).thenReturn(Optional.of(new IdentityProviderEntity()));
        when(identityCacheRepository.findById(sessionId)).thenReturn(Optional.empty());
        ResponseException exception = assertThrows(ResponseException.class, () ->
                identityService.handleCallback(providerKey, sessionId, authorizationCode, origin)
        );
        assertEquals("Die Identitätssitzung existiert nicht.", exception.getMessage());
    }

    @Test
    void handleCallback_ShouldProcessCallbackSuccessfully() throws Exception {
        UUID providerKey = UUID.randomUUID();
        String authorizationCode = "auth-code";
        UUID sessionId = UUID.randomUUID();
        String origin = "https://example.com/origin";
        IdentityProviderEntity provider = new IdentityProviderEntity();
        provider.setKey(providerKey);
        provider.setIsEnabled(true);
        provider.setTokenEndpoint("https://auth.example.com/token");
        provider.setUserinfoEndpoint("https://auth.example.com/userinfo");
        provider.setAttributes(List.of());
        IdentityCacheEntity identity = new IdentityCacheEntity(sessionId, null, providerKey.toString(), "meta", null);
        when(identityProviderService.retrieve(providerKey)).thenReturn(Optional.of(provider));
        when(identityCacheRepository.findById(sessionId)).thenReturn(Optional.of(identity));
        // Mock token endpoint
        String tokenResponse = """
        {"access_token": "access-token", "refresh_token": "refresh-token", "expires_in": 3600}
        """;
        var mockTokenResponse = mock(HttpResponse.class);
        when(mockTokenResponse.statusCode()).thenReturn(200);
        when(mockTokenResponse.body()).thenReturn(tokenResponse);
        when(httpService.postFormUrlEncoded(any(URI.class), anyMap())).thenReturn(mockTokenResponse);
        // Mock user info endpoint
        String userInfoResponse = """
        {"name": "John Doe", "email": "john.doe@example.com"}
        """;
        var mockUserInfoResponse = mock(HttpResponse.class);
        when(mockUserInfoResponse.statusCode()).thenReturn(200);
        when(mockUserInfoResponse.body()).thenReturn(userInfoResponse);
        when(httpService.get(any(URI.class), any(HttpServiceHeaders.class))).thenReturn(mockUserInfoResponse);
        when(identityCacheRepository.save(any(IdentityCacheEntity.class))).thenReturn(identity);
        String result = identityService.handleCallback(providerKey, sessionId, authorizationCode, origin);
        assertNotNull(result);
        assertTrue(result.contains(sessionId.toString()));
        assertTrue(result.contains("identity-state=0"));
    }

    @Test
    void handleCallback_ShouldThrowException_WhenTokenRetrievalFails() throws ResponseException, HttpConnectionException {
        UUID providerKey = UUID.randomUUID();
        String authorizationCode = "auth-code";
        UUID sessionId = UUID.randomUUID();
        String origin = "https://example.com/origin";
        IdentityProviderEntity provider = new IdentityProviderEntity();
        provider.setKey(providerKey);
        provider.setIsEnabled(true);
        provider.setTokenEndpoint("https://auth.example.com/token");
        provider.setAttributes(List.of());
        when(identityProviderService.retrieve(providerKey)).thenReturn(Optional.of(provider));
        IdentityCacheEntity identity = new IdentityCacheEntity(sessionId, null, providerKey.toString(), "meta", null);
        when(identityCacheRepository.findById(sessionId)).thenReturn(Optional.of(identity));
        var mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(400);
        when(mockResponse.body()).thenReturn("Bad Request");
        when(httpService.postFormUrlEncoded(
                eq(URI.create("https://auth.example.com/token")),
                anyMap()
        )).thenReturn(mockResponse);
        ResponseException exception = assertThrows(ResponseException.class, () ->
                identityService.handleCallback(providerKey, sessionId, authorizationCode, origin)
        );
        assertEquals("Ungültiger Status-Code beim Abrufen des Zugriffsschlüssels für Nutzerkontenanbieter null (" + providerKey + "): 400", exception.getMessage());
    }

    @Test
    void handleCallback_ShouldPerformLogoutSuccessfully() throws ResponseException, HttpConnectionException {
        UUID providerKey = UUID.randomUUID();
        String authorizationCode = "auth-code";
        UUID sessionId = UUID.randomUUID();
        String origin = "https://example.com/origin";
        IdentityProviderEntity provider = new IdentityProviderEntity();
        provider.setKey(providerKey);
        provider.setIsEnabled(true);
        provider.setTokenEndpoint("https://auth.example.com/token");
        provider.setUserinfoEndpoint("https://auth.example.com/userinfo");
        provider.setEndSessionEndpoint("https://auth.example.com/logout");
        provider.setAttributes(List.of());
        String tokenResponse = """
        {"access_token": "access-token", "refresh_token": "refresh-token", "expires_in": 3600}
        """;
        String userInfoResponse = """
        {"name": "John Doe", "email": "john.doe@example.com"}
        """;
        when(identityProviderService.retrieve(providerKey)).thenReturn(Optional.of(provider));
        IdentityCacheEntity identity = new IdentityCacheEntity(sessionId, null, providerKey.toString(), "meta", null);
        when(identityCacheRepository.findById(sessionId)).thenReturn(Optional.of(identity));
        var mockTokenResponse = mock(HttpResponse.class);
        when(mockTokenResponse.statusCode()).thenReturn(200);
        when(mockTokenResponse.body()).thenReturn(tokenResponse);
        when(httpService.postFormUrlEncoded(
                eq(URI.create("https://auth.example.com/token")),
                anyMap()
        )).thenReturn(mockTokenResponse);
        var mockUserInfoResponse = mock(HttpResponse.class);
        when(mockUserInfoResponse.statusCode()).thenReturn(200);
        when(mockUserInfoResponse.body()).thenReturn(userInfoResponse);
        when(httpService.get(
                eq(URI.create("https://auth.example.com/userinfo")),
                any(HttpServiceHeaders.class)
        )).thenReturn(mockUserInfoResponse);
        var mockLogoutResponse = mock(HttpResponse.class);
        when(mockLogoutResponse.statusCode()).thenReturn(204);
        when(httpService.postFormUrlEncoded(
                eq(URI.create("https://auth.example.com/logout")),
                anyMap(),
                any(HttpServiceHeaders.class)
        )).thenReturn(mockLogoutResponse);
        when(identityCacheRepository.save(any(IdentityCacheEntity.class))).thenReturn(identity);
        String result = identityService
                .handleCallback(providerKey, sessionId, authorizationCode, origin);
        assertNotNull(result);
        String expectedUrl = UriComponentsBuilder
                .fromUriString(origin)
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
        String authorizationCode = "auth-code";
        UUID sessionId = UUID.randomUUID();
        String origin = "https://example.com/origin";
        IdentityProviderEntity provider = new IdentityProviderEntity();
        provider.setKey(providerKey);
        provider.setIsEnabled(true);
        provider.setTokenEndpoint("https://auth.example.com/token");
        provider.setUserinfoEndpoint("https://auth.example.com/userinfo");
        provider.setClientSecretKey(UUID.randomUUID());
        provider.setAttributes(List.of());
        String decryptedSecret = "decrypted-client-secret";
        String tokenResponse = """
        {"access_token": "access-token", "refresh_token": "refresh-token", "expires_in": 3600}
        """;
        String userInfoResponse = """
        {"name": "John Doe", "email": "john.doe@example.com"}
        """;
        when(identityProviderService.retrieve(providerKey)).thenReturn(Optional.of(provider));
        var dummySecret = new SecretEntity();
        when(secretService.retrieve(provider.getClientSecretKey())).thenReturn(Optional.of(dummySecret));
        when(secretService.decrypt(dummySecret)).thenReturn(decryptedSecret);
        IdentityCacheEntity identity = new IdentityCacheEntity(sessionId, null, providerKey.toString(), "meta", null);
        when(identityCacheRepository.findById(sessionId)).thenReturn(Optional.of(identity));
        var mockTokenResponse = mock(HttpResponse.class);
        when(mockTokenResponse.statusCode()).thenReturn(200);
        when(mockTokenResponse.body()).thenReturn(tokenResponse);
        when(httpService.postFormUrlEncoded(
                eq(URI.create("https://auth.example.com/token")),
                argThat(body -> decryptedSecret.equals(body.get("client_secret")))
        )).thenReturn(mockTokenResponse);
        var mockUserInfoResponse = mock(HttpResponse.class);
        when(mockUserInfoResponse.statusCode()).thenReturn(200);
        when(mockUserInfoResponse.body()).thenReturn(userInfoResponse);
        when(httpService.get(
                eq(URI.create("https://auth.example.com/userinfo")),
                any(HttpServiceHeaders.class)
        )).thenReturn(mockUserInfoResponse);
        when(identityCacheRepository.save(any(IdentityCacheEntity.class))).thenReturn(identity);
        String result = identityService.handleCallback(providerKey, sessionId, authorizationCode, origin);
        assertNotNull(result);
        String expectedUrl = UriComponentsBuilder
                .fromUriString(origin)
                .queryParam("identity-state", "0")
                .queryParam("identity-id", identity.getSessionId())
                .build()
                .toString();
        assertEquals(expectedUrl, result);
        verify(secretService).retrieve(provider.getClientSecretKey());
        verify(secretService).decrypt(dummySecret);
    }
}
