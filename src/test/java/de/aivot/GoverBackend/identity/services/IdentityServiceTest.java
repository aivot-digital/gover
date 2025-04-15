package de.aivot.GoverBackend.identity.services;

import de.aivot.GoverBackend.core.services.HttpService;
import de.aivot.GoverBackend.identity.cache.entities.IdentityCacheEntity;
import de.aivot.GoverBackend.identity.cache.repositories.IdentityCacheRepository;
import de.aivot.GoverBackend.identity.entities.IdentityProviderEntity;
import de.aivot.GoverBackend.identity.models.IdentityAdditionalParameter;
import de.aivot.GoverBackend.identity.models.IdentityAuthTokenData;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.secrets.entities.SecretEntity;
import de.aivot.GoverBackend.secrets.services.SecretService;
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

    @BeforeEach
    void setUp() {
        goverConfig = mock(GoverConfig.class);
        identityProviderService = mock(IdentityProviderService.class);
        identityCacheRepository = mock(IdentityCacheRepository.class);
        httpService = mock(HttpService.class);
        secretService = mock(SecretService.class);
        identityService = new IdentityService(goverConfig, secretService, httpService, identityProviderService, identityCacheRepository);
    }

    @Test
    void createRedirectURL_ShouldConstructValidURL() throws ResponseException {
        String providerKey = "valid-provider";
        URI callbackBaseUrl = URI.create("https://example.com/callback");
        String origin = "https://example.com";
        List<String> additionalScopes = List.of("scope3");

        IdentityProviderEntity provider = new IdentityProviderEntity();
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

        URI result = identityService.createRedirectURL(providerKey, callbackBaseUrl, origin, additionalScopes);

        String expectedUrl = UriComponentsBuilder
                .fromUriString("https://auth.example.com/authorize")
                .queryParam("client_id", "client-id")
                .queryParam("response_type", "code")
                .queryParam("login", "true")
                .queryParam("redirect_uri", "https://example.com/callback?origin=https://example.com")
                .queryParam("scope", "scope1%20scope2%20scope3")
                .queryParam("param1", "value1")
                .queryParam("param2", "value2")
                .build()
                .toString();

        assertEquals(expectedUrl, result.toString());
    }

    @Test
    void createRedirectURL_ShouldThrowException_WhenProviderKeyIsNull() {
        URI callbackBaseUrl = URI.create("https://example.com/callback");
        String origin = "https://example.com";

        ResponseException exception = assertThrows(ResponseException.class, () ->
                identityService.createRedirectURL(null, callbackBaseUrl, origin, null)
        );

        assertEquals("Der Nutzerkontenanbieter ist nicht angegeben.", exception.getMessage());
    }

    @Test
    void createRedirectURL_ShouldThrowException_WhenProviderIsDisabled() throws ResponseException {
        String providerKey = "disabled-provider";
        URI callbackBaseUrl = URI.create("https://example.com/callback");
        String origin = "https://example.com";

        IdentityProviderEntity provider = new IdentityProviderEntity();
        provider.setIsEnabled(false);

        when(identityProviderService.retrieve(providerKey)).thenReturn(Optional.of(provider));

        ResponseException exception = assertThrows(ResponseException.class, () ->
                identityService.createRedirectURL(providerKey, callbackBaseUrl, origin, null)
        );

        assertEquals("Der Nutzerkontenanbieter ist nicht aktiviert.", exception.getMessage());
    }

    @Test
    void createRedirectURL_ShouldThrowException_WhenOriginIsInvalid() throws ResponseException {
        String providerKey = "valid-provider";
        URI callbackBaseUrl = URI.create("https://example.com/callback");
        String invalidOrigin = "invalid-origin";

        IdentityProviderEntity provider = new IdentityProviderEntity();
        provider.setIsEnabled(true);

        when(identityProviderService.retrieve(providerKey)).thenReturn(Optional.of(provider));

        when(goverConfig.getGoverHostname()).thenReturn("https://example.com");

        ResponseException exception = assertThrows(ResponseException.class, () ->
                identityService.createRedirectURL(providerKey, callbackBaseUrl, invalidOrigin, null)
        );

        assertEquals("Der Referer-Header ist ungültig.", exception.getMessage());
    }

    @Test
    void createRedirectURL_ShouldCombineScopesCorrectly() throws ResponseException {
        String providerKey = "valid-provider";
        URI callbackBaseUrl = URI.create("https://example.com/callback");
        String origin = "https://example.com";
        List<String> additionalScopes = List.of("scope2", "scope3");

        IdentityProviderEntity provider = new IdentityProviderEntity();
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

        URI result = identityService.createRedirectURL(providerKey, callbackBaseUrl, origin, additionalScopes);

        assertTrue(result.toString().contains("scope=scope1%20scope2%20scope3"));
    }

    @Test
    void handleCallback_ShouldThrowException_WhenAuthorizationCodeIsNull() {
        String providerKey = "valid-provider";
        URI callbackUrl = URI.create("https://example.com/callback");

        ResponseException exception = assertThrows(ResponseException.class, () ->
                identityService.handleCallback(providerKey, null, callbackUrl, "https://example.com/origin")
        );

        assertEquals("Es wurde kein Autorisierungscode übergeben.", exception.getMessage());
    }

    @Test
    void handleCallback_ShouldThrowException_WhenProviderKeyIsInvalid() throws ResponseException {
        String providerKey = "invalid-provider";
        String authorizationCode = "auth-code";
        URI callbackUrl = URI.create("https://example.com/callback");

        when(identityProviderService.retrieve(providerKey)).thenReturn(Optional.empty());

        ResponseException exception = assertThrows(ResponseException.class, () ->
                identityService.handleCallback(providerKey, authorizationCode, callbackUrl, "https://example.com/origin")
        );

        assertEquals("Der Nutzerkontenanbieter existiert nicht.", exception.getMessage());
    }

    @Test
    void handleCallback_ShouldThrowException_WhenProviderIsDisabled() throws ResponseException {
        String providerKey = "disabled-provider";
        String authorizationCode = "auth-code";
        URI callbackUrl = URI.create("https://example.com/callback");

        IdentityProviderEntity provider = new IdentityProviderEntity();
        provider.setIsEnabled(false);

        when(identityProviderService.retrieve(providerKey)).thenReturn(Optional.of(provider));

        ResponseException exception = assertThrows(ResponseException.class, () ->
                identityService.handleCallback(providerKey, authorizationCode, callbackUrl, "https://example.com/origin")
        );

        assertEquals("Der Nutzerkontenanbieter ist nicht aktiviert.", exception.getMessage());
    }

    @Test
    void handleCallback_ShouldProcessCallbackSuccessfully() throws ResponseException, IOException, InterruptedException {
        String providerKey = "valid-provider";
        String authorizationCode = "auth-code";
        URI callbackUrl = URI.create("https://example.com/callback");

        IdentityProviderEntity provider = new IdentityProviderEntity();
        provider.setKey(providerKey);
        provider.setIsEnabled(true);
        provider.setTokenEndpoint("https://auth.example.com/token");
        provider.setUserinfoEndpoint("https://auth.example.com/userinfo");
        provider.setAttributes(List.of());

        String tokenResponse = """
        {
            "access_token": "access-token",
            "refresh_token": "refresh-token",
            "expires_in": 3600
        }
    """;

        String userInfoResponse = """
        {
            "name": "John Doe",
            "email": "john.doe@example.com"
        }
    """;

        when(identityProviderService.retrieve(providerKey)).thenReturn(Optional.of(provider));

        // Mock the token endpoint response
        var mockTokenResponse = mock(HttpResponse.class);
        when(mockTokenResponse.statusCode()).thenReturn(200);
        when(mockTokenResponse.body()).thenReturn(tokenResponse);
        when(httpService.postFormUrlEncoded(
                eq(URI.create("https://auth.example.com/token")),
                anyMap()
        )).thenReturn(mockTokenResponse);

        // Mock the user info endpoint response
        var mockUserInfoResponse = mock(HttpResponse.class);
        when(mockUserInfoResponse.statusCode()).thenReturn(200);
        when(mockUserInfoResponse.body()).thenReturn(userInfoResponse);
        when(httpService.get(
                eq(URI.create("https://auth.example.com/userinfo")),
                anyMap()
        )).thenReturn(mockUserInfoResponse);

        IdentityCacheEntity savedEntity = new IdentityCacheEntity()
                .setId(UUID.randomUUID().toString())
                .setIdentityData(Map.of("name", "John Doe", "email", "john.doe@example.com"))
                .setProviderKey(providerKey);

        when(identityCacheRepository.save(any(IdentityCacheEntity.class))).thenReturn(savedEntity);

        IdentityCacheEntity result = identityService
                .handleCallback(providerKey, authorizationCode, callbackUrl, "https://example.com/origin");

        assertNotNull(result);
        assertEquals("John Doe", result.getIdentityData().get("name"));
        assertEquals("john.doe@example.com", result.getIdentityData().get("email"));
        assertEquals(providerKey, result.getProviderKey());
    }

    @Test
    void handleCallback_ShouldThrowException_WhenTokenRetrievalFails() throws ResponseException, IOException, InterruptedException {
        String providerKey = "valid-provider";
        String authorizationCode = "auth-code";
        URI callbackUrl = URI.create("https://example.com/callback");

        IdentityProviderEntity provider = new IdentityProviderEntity();
        provider.setKey(providerKey);
        provider.setIsEnabled(true);
        provider.setTokenEndpoint("https://auth.example.com/token");
        provider.setAttributes(List.of());

        when(identityProviderService.retrieve(providerKey)).thenReturn(Optional.of(provider));


        var mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(400);
        when(mockResponse.body()).thenReturn("Bad Request");

        // Mock a failure in the token endpoint response
        when(httpService.postFormUrlEncoded(
                eq(URI.create("https://auth.example.com/token")),
                anyMap()
        )).thenReturn(mockResponse);

        ResponseException exception = assertThrows(ResponseException.class, () ->
                identityService.handleCallback(providerKey, authorizationCode, callbackUrl, "https://example.com/origin")
        );

        assertEquals("Ungültiger Status-Code beim Abrufen des Zugriffsschlüssels für Nutzerkontenanbieter null (valid-provider): 400", exception.getMessage());
    }

    @Test
    void handleCallback_ShouldPerformLogoutSuccessfully() throws ResponseException, IOException, InterruptedException {
        String providerKey = "valid-provider";
        String authorizationCode = "auth-code";
        URI callbackUrl = URI.create("https://example.com/callback");

        IdentityProviderEntity provider = new IdentityProviderEntity();
        provider.setKey(providerKey);
        provider.setIsEnabled(true);
        provider.setTokenEndpoint("https://auth.example.com/token");
        provider.setUserinfoEndpoint("https://auth.example.com/userinfo");
        provider.setEndSessionEndpoint("https://auth.example.com/logout");
        provider.setAttributes(List.of());

        String tokenResponse = """
        {
            "access_token": "access-token",
            "refresh_token": "refresh-token",
            "expires_in": 3600
        }
    """;

        String userInfoResponse = """
        {
            "name": "John Doe",
            "email": "john.doe@example.com"
        }
    """;

        when(identityProviderService.retrieve(providerKey)).thenReturn(Optional.of(provider));

        // Mock the token endpoint response
        var mockTokenResponse = mock(HttpResponse.class);
        when(mockTokenResponse.statusCode()).thenReturn(200);
        when(mockTokenResponse.body()).thenReturn(tokenResponse);
        when(httpService.postFormUrlEncoded(
                eq(URI.create("https://auth.example.com/token")),
                anyMap()
        )).thenReturn(mockTokenResponse);

        // Mock the user info endpoint response
        var mockUserInfoResponse = mock(HttpResponse.class);
        when(mockUserInfoResponse.statusCode()).thenReturn(200);
        when(mockUserInfoResponse.body()).thenReturn(userInfoResponse);
        when(httpService.get(
                eq(URI.create("https://auth.example.com/userinfo")),
                anyMap()
        )).thenReturn(mockUserInfoResponse);

        // Mock the logout endpoint response
        var mockLogoutResponse = mock(HttpResponse.class);
        when(mockLogoutResponse.statusCode()).thenReturn(204);
        when(httpService.postFormUrlEncoded(
                eq(URI.create("https://auth.example.com/logout")),
                anyMap(),
                anyMap()
        )).thenReturn(mockLogoutResponse);

        IdentityCacheEntity savedEntity = new IdentityCacheEntity()
                .setId(UUID.randomUUID().toString())
                .setIdentityData(Map.of("name", "John Doe", "email", "john.doe@example.com"))
                .setProviderKey(providerKey);

        when(identityCacheRepository.save(any(IdentityCacheEntity.class))).thenReturn(savedEntity);

        IdentityCacheEntity result = identityService.handleCallback(providerKey, authorizationCode, callbackUrl, "https://example.com/origin");

        assertNotNull(result);
        assertEquals("John Doe", result.getIdentityData().get("name"));
        assertEquals("john.doe@example.com", result.getIdentityData().get("email"));
        assertEquals(providerKey, result.getProviderKey());

        // Verify that the logout endpoint was called
        verify(httpService).postFormUrlEncoded(
                eq(URI.create("https://auth.example.com/logout")),
                anyMap(),
                anyMap()
        );
    }

    @Test
    void handleCallback_ShouldRetrieveAndUseClientSecret() throws Exception {
        String providerKey = "valid-provider";
        String authorizationCode = "auth-code";
        URI callbackUrl = URI.create("https://example.com/callback");

        IdentityProviderEntity provider = new IdentityProviderEntity();
        provider.setKey(providerKey);
        provider.setIsEnabled(true);
        provider.setTokenEndpoint("https://auth.example.com/token");
        provider.setUserinfoEndpoint("https://auth.example.com/userinfo");
        provider.setClientSecretKey("secret-key");
        provider.setAttributes(List.of());

        String decryptedSecret = "decrypted-client-secret";

        String tokenResponse = """
        {
            "access_token": "access-token",
            "refresh_token": "refresh-token",
            "expires_in": 3600
        }
    """;

        String userInfoResponse = """
        {
            "name": "John Doe",
            "email": "john.doe@example.com"
        }
    """;

        when(identityProviderService.retrieve(providerKey)).thenReturn(Optional.of(provider));
        var dummySecret = new SecretEntity();
        when(secretService.retrieve("secret-key")).thenReturn(Optional.of(dummySecret));
        when(secretService.decrypt(dummySecret)).thenReturn(decryptedSecret);

        // Mock the token endpoint response
        var mockTokenResponse = mock(HttpResponse.class);
        when(mockTokenResponse.statusCode()).thenReturn(200);
        when(mockTokenResponse.body()).thenReturn(tokenResponse);
        when(httpService.postFormUrlEncoded(
                eq(URI.create("https://auth.example.com/token")),
                argThat(body -> decryptedSecret.equals(body.get("client_secret")))
        )).thenReturn(mockTokenResponse);

        // Mock the user info endpoint response
        var mockUserInfoResponse = mock(HttpResponse.class);
        when(mockUserInfoResponse.statusCode()).thenReturn(200);
        when(mockUserInfoResponse.body()).thenReturn(userInfoResponse);
        when(httpService.get(
                eq(URI.create("https://auth.example.com/userinfo")),
                anyMap()
        )).thenReturn(mockUserInfoResponse);

        IdentityCacheEntity savedEntity = new IdentityCacheEntity()
                .setId(UUID.randomUUID().toString())
                .setIdentityData(Map.of("name", "John Doe", "email", "john.doe@example.com"))
                .setProviderKey(providerKey);

        when(identityCacheRepository.save(any(IdentityCacheEntity.class))).thenReturn(savedEntity);

        IdentityCacheEntity result = identityService.handleCallback(providerKey, authorizationCode, callbackUrl, "https://example.com/origin");

        assertNotNull(result);
        assertEquals("John Doe", result.getIdentityData().get("name"));
        assertEquals("john.doe@example.com", result.getIdentityData().get("email"));
        assertEquals(providerKey, result.getProviderKey());

        // Verify that the client secret was retrieved and decrypted
        verify(secretService).retrieve("secret-key");
        verify(secretService).decrypt(dummySecret);
    }
}