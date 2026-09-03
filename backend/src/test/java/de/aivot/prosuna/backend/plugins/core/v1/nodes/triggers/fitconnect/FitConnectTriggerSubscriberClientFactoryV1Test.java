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

class FitConnectTriggerSubscriberClientFactoryV1Test {
    private final SecretService secretService = mock(SecretService.class);
    private final StorageService storageService = mock(StorageService.class);
    private final FitConnectTriggerSubscriberClientFactoryV1 factory =
            new FitConnectTriggerSubscriberClientFactoryV1(secretService, storageService);

    @Test
    void onlineServiceDestinationDoesNotRequirePrivateKeys() throws Exception {
        var config = validConfig(FitConnectTriggerConfigV1.DESTINATION_TYPE_OPTION_ONLINE_SERVICE);

        var issues = factory.validateConfiguration(config);

        assertTrue(issues.isEmpty());
        verifyNoInteractions(storageService);
    }

    @Test
    void administrationDestinationRequiresPrivateKeys() throws Exception {
        var config = validConfig(FitConnectTriggerConfigV1.DESTINATION_TYPE_OPTION_ADMINISTRATION);

        var issues = factory.validateConfiguration(config);

        assertEquals(
                List.of(
                        FitConnectTriggerConfigV1.PRIVATE_SIGNING_KEY_CONFIG_KEY,
                        FitConnectTriggerConfigV1.PRIVATE_DECRYPTION_KEYS_CONFIG_KEY
                ),
                issues.stream().map(FitConnectTriggerSubscriberClientFactoryV1.ValidationIssue::fieldId).toList()
        );
        verifyNoInteractions(storageService);
    }

    @Test
    void missingOrUnknownDestinationTypeIsRejectedWithoutValidatingPrivateKeys() throws Exception {
        for (var destinationType : new String[]{null, "unknown"}) {
            var config = validConfig(destinationType);

            var issues = factory.validateConfiguration(config);

            assertEquals(
                    List.of(FitConnectTriggerConfigV1.DESTINATION_TYPE_CONFIG_KEY),
                    issues.stream().map(FitConnectTriggerSubscriberClientFactoryV1.ValidationIssue::fieldId).toList()
            );
        }
        verifyNoInteractions(storageService);
    }

    private FitConnectTriggerConfigV1 validConfig(String destinationType) throws Exception {
        var subscriberClientSecretId = UUID.randomUUID();
        var callbackSecretId = UUID.randomUUID();
        var secret = mock(SecretEntity.class);
        when(secretService.retrieve(subscriberClientSecretId)).thenReturn(Optional.of(secret));
        when(secretService.retrieve(callbackSecretId)).thenReturn(Optional.of(secret));
        when(secretService.decrypt(secret)).thenReturn("decrypted-secret");

        var config = new FitConnectTriggerConfigV1();
        config.destinationType = destinationType;
        config.subscriberClientId = "subscriber-client";
        config.subscriberClientSecret = subscriberClientSecretId.toString();
        config.callbackSecret = callbackSecretId.toString();
        return config;
    }
}
