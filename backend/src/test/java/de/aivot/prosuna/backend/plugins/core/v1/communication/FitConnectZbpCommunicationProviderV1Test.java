package de.aivot.prosuna.backend.plugins.core.v1.communication;

import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.communication.entities.CommunicationProviderBindingEntity;
import de.aivot.prosuna.backend.communication.entities.CommunicationProviderEntity;
import de.aivot.prosuna.backend.communication.models.CommunicationProviderContext;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SecretSelectInputElement;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityProviderType;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.secrets.entities.SecretEntity;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import de.aivot.prosuna.backend.storage.services.StorageService;
import dev.fitko.fitconnect.api.config.ApplicationConfig;
import dev.fitko.fitconnect.api.domain.zbp.message.AuthenticationLevel;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FitConnectZbpCommunicationProviderV1Test {
    private final SecretService secretService = mock(SecretService.class);
    private final FitConnectZbpCommunicationProviderV1 definition = new FitConnectZbpCommunicationProviderV1(
            mock(StorageService.class),
            secretService
    );

    @Test
    void configLayoutUsesSecretSelectionWithoutLoadingSecretOptions() throws Exception {
        var layout = definition.getConfigLayout();

        assertTrue(layout.findChild(
                FitConnectZbpCommunicationProviderV1.Config.SENDER_CLIENT_SECRET_KEY_FIELD_ID,
                SecretSelectInputElement.class
        ).isPresent());
        verifyNoInteractions(secretService);
    }

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

    @Test
    void authenticationLevelUsesConfiguredAttributeForCustomIdentityProvider() {
        var attributes = Map.of(
                "configured_qaa", "level3",
                "trust_level_authentication", "level4"
        );

        var authenticationLevel = mapAuthenticationLevel("configured_qaa", attributes);

        assertEquals(AuthenticationLevel.THREE, authenticationLevel);
    }

    @Test
    void authenticationLevelMapsKnownValuesAndFallsBackForUnknownValue() {
        assertEquals(AuthenticationLevel.ONE, mapAuthenticationLevel("qaa", Map.of("qaa", "level1")));
        assertEquals(AuthenticationLevel.TWO, mapAuthenticationLevel("qaa", Map.of("qaa", "level2")));
        assertEquals(AuthenticationLevel.THREE, mapAuthenticationLevel("qaa", Map.of("qaa", "level3")));
        assertEquals(AuthenticationLevel.FOUR, mapAuthenticationLevel("qaa", Map.of("qaa", "level4")));
        assertEquals(AuthenticationLevel.ONE, mapAuthenticationLevel("qaa", Map.of("qaa", "unknown")));
        assertEquals(AuthenticationLevel.ONE, mapAuthenticationLevel("qaa", Map.of("qaa", "")));
    }

    @Test
    void authenticationLevelFallsBackWhenBindingAttributeIsMissing() {
        var attributes = Map.of("qaa", "level4");

        assertEquals(AuthenticationLevel.ONE, mapAuthenticationLevel(null, attributes));
        assertEquals(AuthenticationLevel.ONE, mapAuthenticationLevel("", attributes));
        assertEquals(AuthenticationLevel.ONE, mapAuthenticationLevel(" ", attributes));
    }

    @Test
    void authenticationLevelFallsBackWhenIdentityAttributeIsMissing() {
        assertEquals(AuthenticationLevel.ONE, mapAuthenticationLevel("qaa", Map.of()));
    }

    private ApplicationConfig getApplicationConfig(FitConnectZbpCommunicationProviderV1.Config config) throws CommunicationException {
        try {
            return ReflectionTestUtils.invokeMethod(definition, "getApplicationConfig", config);
        } catch (UndeclaredThrowableException e) {
            if (e.getUndeclaredThrowable() instanceof CommunicationException communicationException) {
                throw communicationException;
            }
            throw e;
        }
    }

    private AuthenticationLevel mapAuthenticationLevel(String attributeKey, Map<String, String> attributes) {
        var bindingConfig = new FitConnectZbpCommunicationProviderV1.IdentityBinding();
        bindingConfig.storkQaaLevel = attributeKey;
        var context = new CommunicationProviderContext<>(
                mock(CommunicationProviderEntity.class),
                new IdentityProviderEntity().setType(IdentityProviderType.Custom),
                mock(CommunicationProviderBindingEntity.class),
                new FitConnectZbpCommunicationProviderV1.Config(),
                bindingConfig
        );
        var identity = new IdentityData(
                "session-id",
                "identity-id",
                IdentityType.IdentityProvider,
                UUID.randomUUID(),
                "custom",
                null,
                attributes,
                null,
                Map.of()
        );

        return ReflectionTestUtils.invokeMethod(definition, "mapAuthenticationLevel", context, identity);
    }

    private static FitConnectZbpCommunicationProviderV1.Config config(String clientId, String secretKey) {
        var config = new FitConnectZbpCommunicationProviderV1.Config();
        config.senderClientId = clientId;
        config.senderClientSecret = secretKey;
        return config;
    }
}
