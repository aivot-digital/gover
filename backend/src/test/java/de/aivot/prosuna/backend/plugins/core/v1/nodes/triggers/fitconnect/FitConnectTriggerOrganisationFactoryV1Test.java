package de.aivot.prosuna.backend.plugins.core.v1.nodes.triggers.fitconnect;

import de.aivot.prosuna.backend.secrets.entities.SecretEntity;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import de.aivot.prosuna.backend.storage.services.StorageService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FitConnectTriggerOrganisationFactoryV1Test {
    private final SecretService secretService = mock(SecretService.class);
    private final StorageService storageService = mock(StorageService.class);
    private final FitConnectTriggerOrganisationFactoryV1 factory =
            new FitConnectTriggerOrganisationFactoryV1(secretService, storageService);

    @Test
    void missingPrivateKeysAreRejected() throws Exception {
        var config = validConfig();
        config.privateDecryptionKeys = null;

        var issues = factory.validateConfiguration(config);

        assertEquals(
                List.of(
                        FitConnectTriggerConfigV1.PRIVATE_SIGNING_KEY_CONFIG_KEY,
                        FitConnectTriggerConfigV1.PRIVATE_DECRYPTION_KEYS_CONFIG_KEY
                ),
                issues.stream().map(FitConnectTriggerOrganisationFactoryV1.ValidationIssue::fieldId).toList()
        );
        verifyNoInteractions(storageService);
    }

    @Test
    void emptyPrivateDecryptionKeyListIsRejected() throws Exception {
        var config = validConfig();

        var issues = factory.validateConfiguration(config);

        assertTrue(issues.stream().anyMatch(
                issue -> FitConnectTriggerConfigV1.PRIVATE_DECRYPTION_KEYS_CONFIG_KEY.equals(issue.fieldId())
        ));
        verifyNoInteractions(storageService);
    }

    private FitConnectTriggerConfigV1 validConfig() throws Exception {
        var subscriberClientSecretId = UUID.randomUUID();
        var callbackSecretId = UUID.randomUUID();
        var secret = mock(SecretEntity.class);
        when(secretService.retrieve(subscriberClientSecretId)).thenReturn(Optional.of(secret));
        when(secretService.retrieve(callbackSecretId)).thenReturn(Optional.of(secret));
        when(secretService.decrypt(secret)).thenReturn("decrypted-secret");

        var config = new FitConnectTriggerConfigV1();
        config.subscriberClientId = "subscriber-client";
        config.subscriberClientSecret = subscriberClientSecretId.toString();
        config.callbackSecret = callbackSecretId.toString();
        return config;
    }
}
