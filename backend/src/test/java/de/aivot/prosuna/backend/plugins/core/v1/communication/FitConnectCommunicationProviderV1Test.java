package de.aivot.prosuna.backend.plugins.core.v1.communication;

import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.secrets.entities.SecretEntity;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import de.aivot.prosuna.backend.storage.services.StorageService;
import dev.fitko.fitconnect.api.config.ApplicationConfig;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FitConnectCommunicationProviderV1Test {
    private final SecretService secretService = mock(SecretService.class);
    private final FitConnectCommunicationProviderV1 definition = new FitConnectCommunicationProviderV1(
            mock(StorageService.class),
            secretService
    );

    @Test
    void applicationConfigUsesConfiguredClientIdAndDecryptedSecret() throws Exception {
        var secretKey = UUID.randomUUID();
        var secretEntity = mock(SecretEntity.class);
        var config = config("sender-client", secretKey.toString());
        when(secretService.retrieve(secretKey)).thenReturn(Optional.of(secretEntity));
        when(secretService.decrypt(secretEntity)).thenReturn("decrypted-secret");

        var applicationConfig = getApplicationConfig(config);

        assertEquals("sender-client", applicationConfig.getSenderConfig().getClientId());
        assertEquals("decrypted-secret", applicationConfig.getSenderConfig().getClientSecret());
        verify(secretService).retrieve(secretKey);
        verify(secretService).decrypt(secretEntity);
    }

    @Test
    void applicationConfigRejectsInvalidSecretKey() {
        var config = config("sender-client", "not-a-uuid");

        var exception = assertThrows(CommunicationException.class, () -> getApplicationConfig(config));

        assertEquals("Failed to parse sender client secret key as UUID: not-a-uuid", exception.getMessage());
        verifyNoInteractions(secretService);
    }

    @Test
    void applicationConfigRejectsMissingSecret() {
        var secretKey = UUID.randomUUID();
        var config = config("sender-client", secretKey.toString());
        when(secretService.retrieve(secretKey)).thenReturn(Optional.empty());

        var exception = assertThrows(CommunicationException.class, () -> getApplicationConfig(config));

        assertEquals("Sender client secret not found: " + secretKey, exception.getMessage());
        verify(secretService).retrieve(secretKey);
    }

    @Test
    void applicationConfigWrapsSecretDecryptionFailure() throws Exception {
        var secretKey = UUID.randomUUID();
        var secretEntity = mock(SecretEntity.class);
        var config = config("sender-client", secretKey.toString());
        var cause = new Exception("decryption failed");
        when(secretService.retrieve(secretKey)).thenReturn(Optional.of(secretEntity));
        when(secretService.decrypt(secretEntity)).thenThrow(cause);

        var exception = assertThrows(CommunicationException.class, () -> getApplicationConfig(config));

        assertEquals("Failed to decrypt sender client secret: " + secretKey, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    private ApplicationConfig getApplicationConfig(FitConnectCommunicationProviderV1.Config config) {
        return ReflectionTestUtils.invokeMethod(definition, "getApplicationConfig", config);
    }

    private static FitConnectCommunicationProviderV1.Config config(String clientId, String secretKey) {
        var config = new FitConnectCommunicationProviderV1.Config();
        config.senderClientId = clientId;
        config.senderClientSecret = secretKey;
        return config;
    }
}
