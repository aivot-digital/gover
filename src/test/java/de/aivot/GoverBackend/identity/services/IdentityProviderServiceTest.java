package de.aivot.GoverBackend.identity.services;

import de.aivot.GoverBackend.asset.entities.AssetEntity;
import de.aivot.GoverBackend.asset.repositories.AssetRepository;
import de.aivot.GoverBackend.core.services.HttpService;
import de.aivot.GoverBackend.form.repositories.FormRepository;
import de.aivot.GoverBackend.identity.entities.IdentityProviderEntity;
import de.aivot.GoverBackend.identity.enums.IdentityProviderType;
import de.aivot.GoverBackend.identity.repositories.IdentityProviderRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.secrets.entities.SecretEntity;
import de.aivot.GoverBackend.secrets.repositories.SecretRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IdentityProviderServiceTest {
    private IdentityProviderService identityProviderService;
    private IdentityProviderRepository identityProviderRepository;
    private SecretRepository secretRepository;
    private AssetRepository assetRepository;
    private HttpService httpService;
    private FormRepository formRepository;

    @BeforeEach
    void setUp() {
        httpService = mock(HttpService.class);
        identityProviderRepository = mock(IdentityProviderRepository.class);
        secretRepository = mock(SecretRepository.class);
        assetRepository = mock(AssetRepository.class);
        formRepository = mock(FormRepository.class);

        identityProviderService = new IdentityProviderService(
                identityProviderRepository,
                secretRepository,
                assetRepository,
                formRepository,
                httpService
        );
    }

    @Test
    void prepare_ValidResponse_ReturnsEntity() throws Exception {
        String endpoint = "https://example.com/.well-known/openid-configuration";
        String jsonResponse = """
                    {
                        "authorization_endpoint": "https://example.com/auth",
                        "token_endpoint": "https://example.com/token",
                        "userinfo_endpoint": "https://example.com/userinfo",
                        "end_session_endpoint": "https://example.com/logout",
                        "scopes_supported": ["openid", "profile", "email"]
                    }
                """;

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode())
                .thenReturn(200);
        when(mockResponse.body())
                .thenReturn(jsonResponse);
        when(httpService.get(any(URI.class)))
                .thenReturn(mockResponse);

        IdentityProviderEntity result = identityProviderService.prepare(endpoint);

        assertNotNull(result);
        assertEquals("https://example.com/auth", result.getAuthorizationEndpoint());
        assertEquals("https://example.com/token", result.getTokenEndpoint());
        assertEquals("https://example.com/userinfo", result.getUserinfoEndpoint());
        assertEquals("https://example.com/logout", result.getEndSessionEndpoint());
        assertEquals(List.of("openid", "profile", "email"), result.getDefaultScopes());
    }

    @Test
    void prepare_InvalidStatusCode_ThrowsResponseException() throws Exception {
        String endpoint = "https://example.com/.well-known/openid-configuration";

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode())
                .thenReturn(404);
        when(httpService.get(any(URI.class)))
                .thenReturn(mockResponse);

        ResponseException exception = assertThrows(ResponseException.class, () -> identityProviderService.prepare(endpoint));
        assertTrue(exception.getMessage().contains("Status 404"));
    }

    @Test
    void prepare_InvalidJson_ThrowsResponseException() throws Exception {
        String endpoint = "https://example.com/.well-known/openid-configuration";
        String invalidJson = "invalid-json";

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode())
                .thenReturn(200);
        when(mockResponse.body())
                .thenReturn(invalidJson);
        when(httpService.get(any(URI.class)))
                .thenReturn(mockResponse);

        ResponseException exception = assertThrows(ResponseException.class, () -> identityProviderService.prepare(endpoint));
        assertTrue(exception.getMessage().contains("ungültige Antwort"));
    }

    @Test
    void prepare_HttpServiceThrowsException_ThrowsResponseException() throws Exception {
        String endpoint = "https://example.com/.well-known/openid-configuration";

        when(httpService.get(any(URI.class)))
                .thenThrow(new IOException("Connection error"));

        ResponseException exception = assertThrows(ResponseException.class, () -> identityProviderService.prepare(endpoint));
        assertTrue(exception.getMessage().contains("konnte nicht erreicht werden"));
    }

    @Test
    void create_NotGivenClientSecretKey() throws ResponseException {
        IdentityProviderEntity entity = new IdentityProviderEntity();
        entity.setClientSecretKey(null);

        when(identityProviderRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IdentityProviderEntity result = identityProviderService.create(entity);

        assertNull(result.getClientSecretKey());
        assertNotNull(result.getKey());
        assertEquals(IdentityProviderType.Custom, result.getType());
    }

    @Test
    void create_ExistingClientSecretKey() throws ResponseException {
        String secretKey = UUID.randomUUID().toString();
        IdentityProviderEntity entity = new IdentityProviderEntity();
        entity.setClientSecretKey(secretKey);

        when(secretRepository.findById(secretKey))
                .thenReturn(Optional.of(new SecretEntity()));
        when(identityProviderRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IdentityProviderEntity result = identityProviderService
                .create(entity);

        assertEquals(secretKey, result.getClientSecretKey());
        assertNotNull(result.getKey());
        assertEquals(IdentityProviderType.Custom, result.getType());
    }

    @Test
    void create_NonExistingClientSecretKey() throws ResponseException {
        String secretKey = UUID.randomUUID().toString();
        IdentityProviderEntity entity = new IdentityProviderEntity();
        entity.setClientSecretKey(secretKey);

        when(secretRepository.findById(secretKey))
                .thenReturn(Optional.empty());
        when(identityProviderRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IdentityProviderEntity result = identityProviderService
                .create(entity);

        assertNull(result.getClientSecretKey());
        assertNotNull(result.getKey());
        assertEquals(IdentityProviderType.Custom, result.getType());
    }

    @Test
    void create_NotGivenIconAssetKey() throws ResponseException {
        IdentityProviderEntity entity = new IdentityProviderEntity();
        entity.setIconAssetKey(null);

        when(identityProviderRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IdentityProviderEntity result = identityProviderService
                .create(entity);

        assertNull(result.getIconAssetKey());
        assertNotNull(result.getKey());
        assertEquals(IdentityProviderType.Custom, result.getType());
    }

    @Test
    void create_ExistingIconAssetKey() throws ResponseException {
        String assetKey = UUID.randomUUID().toString();
        IdentityProviderEntity entity = new IdentityProviderEntity();
        entity.setIconAssetKey(assetKey);

        when(assetRepository.findById(assetKey))
                .thenReturn(Optional.of(new AssetEntity()));
        when(identityProviderRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IdentityProviderEntity result = identityProviderService
                .create(entity);

        assertEquals(assetKey, result.getIconAssetKey());
        assertNotNull(result.getKey());
        assertEquals(IdentityProviderType.Custom, result.getType());
    }

    @Test
    void create_NonExistingIconAssetKey() throws ResponseException {
        String assetKey = UUID.randomUUID().toString();
        IdentityProviderEntity entity = new IdentityProviderEntity();
        entity.setIconAssetKey(assetKey);

        when(assetRepository.findById(assetKey))
                .thenReturn(Optional.empty());
        when(identityProviderRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IdentityProviderEntity result = identityProviderService
                .create(entity);

        assertNull(result.getIconAssetKey());
        assertNotNull(result.getKey());
        assertEquals(IdentityProviderType.Custom, result.getType());
    }

    @Test
    void performUpdate_SystemIdentityProvider_UpdatesEnabledFieldOnly() throws ResponseException {
        IdentityProviderEntity existingEntity = new IdentityProviderEntity();
        existingEntity.setKey("system-key");
        existingEntity.setType(IdentityProviderType.BundId);
        existingEntity.setIsEnabled(false); // Initially disabled

        IdentityProviderEntity updatedEntity = new IdentityProviderEntity();
        updatedEntity.setIsEnabled(true); // Attempt to enable the provider

        when(identityProviderRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IdentityProviderEntity result = identityProviderService.performUpdate("id", updatedEntity, existingEntity);

        assertEquals("system-key", result.getKey());
        assertEquals(IdentityProviderType.BundId, result.getType());
        assertTrue(result.getIsEnabled()); // Ensure the enabled field is updated
        verify(identityProviderRepository, times(1)).save(result);
    }

    @Test
    void performUpdate_NotGivenClientSecretKey() throws ResponseException {
        IdentityProviderEntity existingEntity = new IdentityProviderEntity();
        existingEntity.setKey("existing-key");
        existingEntity.setType(IdentityProviderType.Custom);

        IdentityProviderEntity updatedEntity = new IdentityProviderEntity();
        updatedEntity.setClientSecretKey(null);

        when(identityProviderRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IdentityProviderEntity result = identityProviderService
                .performUpdate("id", updatedEntity, existingEntity);

        assertNull(result.getClientSecretKey());
        assertEquals("existing-key", result.getKey());
        assertEquals(IdentityProviderType.Custom, result.getType());
    }

    @Test
    void performUpdate_ExistingClientSecretKey() throws ResponseException {
        String secretKey = UUID.randomUUID().toString();
        IdentityProviderEntity existingEntity = new IdentityProviderEntity();
        existingEntity.setKey("existing-key");
        existingEntity.setType(IdentityProviderType.Custom);

        IdentityProviderEntity updatedEntity = new IdentityProviderEntity();
        updatedEntity.setClientSecretKey(secretKey);

        when(secretRepository.findById(secretKey))
                .thenReturn(Optional.of(new SecretEntity()));
        when(identityProviderRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IdentityProviderEntity result = identityProviderService
                .performUpdate("id", updatedEntity, existingEntity);

        assertEquals(secretKey, result.getClientSecretKey());
        assertEquals("existing-key", result.getKey());
        assertEquals(IdentityProviderType.Custom, result.getType());
    }

    @Test
    void performUpdate_NonExistingClientSecretKey() throws ResponseException {
        String secretKey = UUID.randomUUID().toString();
        IdentityProviderEntity existingEntity = new IdentityProviderEntity();
        existingEntity.setKey("existing-key");
        existingEntity.setType(IdentityProviderType.Custom);

        IdentityProviderEntity updatedEntity = new IdentityProviderEntity();
        updatedEntity.setClientSecretKey(secretKey);

        when(secretRepository.findById(secretKey))
                .thenReturn(Optional.empty());
        when(identityProviderRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IdentityProviderEntity result = identityProviderService
                .performUpdate("id", updatedEntity, existingEntity);

        assertNull(result.getClientSecretKey());
        assertEquals("existing-key", result.getKey());
        assertEquals(IdentityProviderType.Custom, result.getType());
    }

    @Test
    void performUpdate_NotGivenIconAssetKey() throws ResponseException {
        IdentityProviderEntity existingEntity = new IdentityProviderEntity();
        existingEntity.setKey("existing-key");
        existingEntity.setType(IdentityProviderType.Custom);

        IdentityProviderEntity updatedEntity = new IdentityProviderEntity();
        updatedEntity.setIconAssetKey(null);

        when(identityProviderRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IdentityProviderEntity result = identityProviderService
                .performUpdate("id", updatedEntity, existingEntity);

        assertNull(result.getIconAssetKey());
        assertEquals("existing-key", result.getKey());
        assertEquals(IdentityProviderType.Custom, result.getType());
    }

    @Test
    void performUpdate_ExistingIconAssetKey() throws ResponseException {
        String assetKey = UUID.randomUUID().toString();
        IdentityProviderEntity existingEntity = new IdentityProviderEntity();
        existingEntity.setKey("existing-key");
        existingEntity.setType(IdentityProviderType.Custom);

        IdentityProviderEntity updatedEntity = new IdentityProviderEntity();
        updatedEntity.setIconAssetKey(assetKey);

        when(assetRepository.findById(assetKey))
                .thenReturn(Optional.of(new AssetEntity()));
        when(identityProviderRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IdentityProviderEntity result = identityProviderService
                .performUpdate("id", updatedEntity, existingEntity);

        assertEquals(assetKey, result.getIconAssetKey());
        assertEquals("existing-key", result.getKey());
        assertEquals(IdentityProviderType.Custom, result.getType());
    }

    @Test
    void performUpdate_NonExistingIconAssetKey() throws ResponseException {
        String assetKey = UUID.randomUUID().toString();
        IdentityProviderEntity existingEntity = new IdentityProviderEntity();
        existingEntity.setKey("existing-key");
        existingEntity.setType(IdentityProviderType.Custom);

        IdentityProviderEntity updatedEntity = new IdentityProviderEntity();
        updatedEntity.setIconAssetKey(assetKey);

        when(assetRepository.findById(assetKey))
                .thenReturn(Optional.empty());
        when(identityProviderRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IdentityProviderEntity result = identityProviderService
                .performUpdate("id", updatedEntity, existingEntity);

        assertNull(result.getIconAssetKey());
        assertEquals("existing-key", result.getKey());
        assertEquals(IdentityProviderType.Custom, result.getType());
    }

    @Test
    void performDelete_CustomIdentityProviderWithoutLinkedForms() throws ResponseException {
        IdentityProviderEntity entity = new IdentityProviderEntity();
        entity.setKey("custom-key");
        entity.setType(IdentityProviderType.Custom);

        when(formRepository.existsWithLinkedIdentityProvider(entity.getKey())).thenReturn(false);

        assertDoesNotThrow(() -> identityProviderService.performDelete(entity));
        verify(identityProviderRepository, times(1)).delete(entity);
    }

    @Test
    void performDelete_SystemIdentityProvider() {
        IdentityProviderEntity entity = new IdentityProviderEntity();
        entity.setKey("system-key");
        entity.setType(IdentityProviderType.BundId);

        ResponseException exception = assertThrows(ResponseException.class, () -> identityProviderService.performDelete(entity));
        assertEquals("Der Nutzerkontenanbieter null (system-key) ist ein Systemanbieter und kann nicht gelöscht werden.", exception.getMessage());
        verify(identityProviderRepository, never()).delete(entity);
    }

    @Test
    void performDelete_CustomIdentityProviderWithLinkedForms() {
        IdentityProviderEntity entity = new IdentityProviderEntity();
        entity.setKey("custom-key");
        entity.setType(IdentityProviderType.Custom);

        when(formRepository.existsWithLinkedIdentityProvider(entity.getKey())).thenReturn(true);

        ResponseException exception = assertThrows(ResponseException.class, () -> identityProviderService.performDelete(entity));
        assertEquals("Für den Nutzerkontenanbieter null (custom-key) existieren noch Formulare, die diesen Anbieter verwenden. Bitte entfernen Sie diese Verknüpfung, bevor Sie den Anbieter löschen.", exception.getMessage());
        verify(identityProviderRepository, never()).delete(entity);
    }
}