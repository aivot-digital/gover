package de.aivot.prosuna.backend.plugins.core.v1.nodes.triggers.fitconnect;

import de.aivot.prosuna.backend.asset.services.AssetContentResolverService;
import de.aivot.prosuna.backend.elements.enums.AssetVisibility;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.secrets.entities.SecretEntity;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FitConnectTriggerOrganisationFactoryV1Test {
    private final SecretService secretService = mock(SecretService.class);
    private final AssetContentResolverService assetContentResolverService = mock(AssetContentResolverService.class);
    private final FitConnectTriggerOrganisationFactoryV1 factory =
            new FitConnectTriggerOrganisationFactoryV1(secretService, assetContentResolverService);

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void validationLogsUnreadableReferencesAndReturnsOnlyFieldMessages(CapturedOutput output) throws Exception {
        var config = validConfig();
        config.callbackSecret = "malformed-reference";
        config.privateSigningKey = UUID.randomUUID().toString();
        when(secretService.decrypt(secretService.retrieve(UUID.fromString(config.subscriberClientSecret)).orElseThrow()))
                .thenThrow(new IllegalArgumentException("invalid encrypted bytes"));
        when(assetContentResolverService.resolveContent(eq(config.privateSigningKey), eq(AssetVisibility.Private), anyString()))
                .thenThrow(ResponseException.internalServerError("Signaturschlüssel konnte nicht gelesen werden.", new IOException("storage read failed")));

        var issues = factory.validateConfiguration(config);

        assertTrue(issues.contains(new FitConnectTriggerOrganisationFactoryV1.ValidationIssue(
                FitConnectTriggerConfigV1.SUBSCRIBER_CLIENT_SECRET_CONFIG_KEY, "Das ausgewählte Client-Secret konnte nicht entschlüsselt werden.")));
        assertTrue(issues.contains(new FitConnectTriggerOrganisationFactoryV1.ValidationIssue(
                FitConnectTriggerConfigV1.PRIVATE_SIGNING_KEY_CONFIG_KEY, "Signaturschlüssel konnte nicht gelesen werden.")));
        assertTrue(output.getAll().contains("invalid encrypted bytes"));
        assertTrue(output.getAll().contains("storage read failed"));
        assertFalse(output.getAll().contains("malformed-reference"));
    }

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
        verifyNoInteractions(assetContentResolverService);
    }

    @Test
    void emptyPrivateDecryptionKeyListIsRejected() throws Exception {
        var config = validConfig();

        var issues = factory.validateConfiguration(config);

        assertTrue(issues.stream().anyMatch(
                issue -> FitConnectTriggerConfigV1.PRIVATE_DECRYPTION_KEYS_CONFIG_KEY.equals(issue.fieldId())
        ));
        verifyNoInteractions(assetContentResolverService);
    }

    @Test
    void missingSecretsNameTheirConfigurationFields() throws Exception {
        var config = validConfig();
        config.subscriberClientSecret = null;
        config.callbackSecret = null;

        var issues = factory.validateConfiguration(config);

        assertTrue(issues.contains(new FitConnectTriggerOrganisationFactoryV1.ValidationIssue(
                FitConnectTriggerConfigV1.SUBSCRIBER_CLIENT_SECRET_CONFIG_KEY,
                "Wählen Sie ein Geheimnis für „Client-Secret“ aus."
        )));
        assertTrue(issues.contains(new FitConnectTriggerOrganisationFactoryV1.ValidationIssue(
                FitConnectTriggerConfigV1.CALLBACK_SECRET_KEY,
                "Wählen Sie ein Geheimnis für „Callback-Secret“ aus."
        )));
        verifyNoInteractions(assetContentResolverService);
    }

    @Test
    void clientSecretDecryptionFailureKeepsItsCauseWithoutExposingInternalDetails() throws Exception {
        var config = validConfig();
        config.destinationId = UUID.randomUUID().toString();
        var secret = secretService.retrieve(UUID.fromString(config.subscriberClientSecret)).orElseThrow();
        var cause = new Exception("Internal decryption details");
        when(secretService.decrypt(secret)).thenThrow(cause);

        var error = assertThrows(ResponseException.class, () -> factory.create(config));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, error.getStatus());
        assertEquals("Das ausgewählte Client-Secret konnte nicht entschlüsselt werden.", error.getTitle());
        assertNull(error.getDetails());
        assertSame(cause, error.getCause());
        verifyNoInteractions(assetContentResolverService);
    }

    @Test
    void malformedSigningKeyKeepsItsParserCauseWithoutExposingInternalDetails() throws Exception {
        var config = validConfig();
        config.destinationId = UUID.randomUUID().toString();
        config.privateSigningKey = UUID.randomUUID().toString();
        when(assetContentResolverService.resolveContent(
                eq(config.privateSigningKey), eq(AssetVisibility.Private), anyString()
        )).thenReturn("not-json".getBytes(StandardCharsets.UTF_8));

        var error = assertThrows(ResponseException.class, () -> factory.create(config));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, error.getStatus());
        assertEquals("Der ausgewählte private Signaturschlüssel konnte nicht als JWK gelesen werden.", error.getTitle());
        assertNull(error.getDetails());
        assertInstanceOf(ParseException.class, error.getCause());
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
