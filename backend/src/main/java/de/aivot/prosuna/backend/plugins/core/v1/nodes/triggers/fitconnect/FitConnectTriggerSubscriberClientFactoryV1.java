package de.aivot.prosuna.backend.plugins.core.v1.nodes.triggers.fitconnect;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyOperation;
import com.nimbusds.jose.jwk.RSAKey;
import de.aivot.prosuna.backend.elements.models.elements.form.input.StoragePathSelectorInputElementValue;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import de.aivot.prosuna.backend.storage.services.StorageService;
import de.aivot.prosuna.backend.utils.StringUtils;
import dev.fitko.fitconnect.api.config.ApplicationConfig;
import dev.fitko.fitconnect.api.config.EnvironmentName;
import dev.fitko.fitconnect.api.config.SubscriberConfig;
import dev.fitko.fitconnect.client.SubscriberClient;
import dev.fitko.fitconnect.client.bootstrap.ClientFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Builds FIT-Connect subscriber clients from a validated trigger configuration. */
@Service
public class FitConnectTriggerSubscriberClientFactoryV1 {
    private static final Set<String> SUPPORTED_ENVIRONMENTS = Set.of("TEST", "STAGE", "PROD");

    private final SecretService secretService;
    private final StorageService storageService;

    public FitConnectTriggerSubscriberClientFactoryV1(SecretService secretService,
                                                       StorageService storageService) {
        this.secretService = secretService;
        this.storageService = storageService;
    }

    @Nonnull
    public SubscriberClient create(@Nonnull FitConnectTriggerConfigV1 config) throws ResponseException {
        var environment = normalizeEnvironment(config.environment);
        var clientId = requireValue(
                config.subscriberClientId,
                "Die Subscriber-Client-ID des FIT-Connect-Trigger-Knotens ist nicht konfiguriert."
        );
        var clientSecret = resolveSecret(
                config.subscriberClientSecret,
                "Das Subscriber-Client-Secret des FIT-Connect-Trigger-Knotens"
        );
        var signingKey = resolveJwk(
                config.privateSigningKey,
                "Der private Signaturschlüssel des FIT-Connect-Trigger-Knotens"
        );
        var decryptionKeys = resolveDecryptionKeys(config.privateDecryptionKeys);

        var subscriberConfig = SubscriberConfig
                .builder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .privateSigningKey(signingKey)
                .privateDecryptionKeys(decryptionKeys)
                .build();

        var applicationConfig = ApplicationConfig
                .builder()
                .activeEnvironment(new EnvironmentName(environment))
                .subscriberConfig(subscriberConfig)
                .build();

        try {
            return ClientFactory.createSubscriberClient(applicationConfig);
        } catch (Exception e) {
            throw ResponseException.internalServerError(
                    "Der FIT-Connect-Subscriber-Client konnte nicht initialisiert werden.",
                    e
            );
        }
    }

    @Nonnull
    public List<ValidationIssue> validateConfiguration(@Nonnull FitConnectTriggerConfigV1 config) {
        var issues = new ArrayList<ValidationIssue>();

        var normalizedEnvironment = normalizeEnvironmentOrNull(config.environment);
        if (normalizedEnvironment == null || !SUPPORTED_ENVIRONMENTS.contains(normalizedEnvironment)) {
            issues.add(new ValidationIssue(
                    FitConnectTriggerConfigV1.ENVIRONMENT_CONFIG_KEY,
                    "Wählen Sie eine unterstützte FIT-Connect-Umgebung aus."
            ));
        }
        if (StringUtils.toNullableTrimmedString(config.subscriberClientId) == null) {
            issues.add(new ValidationIssue(
                    FitConnectTriggerConfigV1.SUBSCRIBER_CLIENT_ID_CONFIG_KEY,
                    "Die Subscriber-Client-ID muss hinterlegt werden."
            ));
        }

        validateSecretReference(
                config.subscriberClientSecret,
                FitConnectTriggerConfigV1.SUBSCRIBER_CLIENT_SECRET_CONFIG_KEY,
                "Das Subscriber-Client-Secret",
                issues
        );
        validateSecretReference(
                config.callbackSecret,
                FitConnectTriggerConfigV1.CALLBACK_SECRET_KEY,
                "Das Callback-Secret",
                issues
        );
        validateJwkReference(
                config.privateSigningKey,
                FitConnectTriggerConfigV1.PRIVATE_SIGNING_KEY_CONFIG_KEY,
                "Der private Signaturschlüssel",
                KeyOperation.SIGN,
                issues
        );

        if (config.privateDecryptionKeys == null || config.privateDecryptionKeys.isEmpty()) {
            issues.add(new ValidationIssue(
                    FitConnectTriggerConfigV1.PRIVATE_DECRYPTION_KEYS_CONFIG_KEY,
                    "Mindestens ein privater Entschlüsselungsschlüssel muss hinterlegt werden."
            ));
        } else {
            for (var keyConfig : config.privateDecryptionKeys) {
                validateJwkReference(
                        keyConfig == null ? null : keyConfig.keyFile,
                        FitConnectTriggerConfigV1.PRIVATE_DECRYPTION_KEYS_CONFIG_KEY,
                        "Der private Entschlüsselungsschlüssel",
                        KeyOperation.UNWRAP_KEY,
                        issues
                );
            }
        }

        return issues;
    }

    @Nonnull
    private String normalizeEnvironment(@Nullable String rawEnvironment) throws ResponseException {
        var environment = normalizeEnvironmentOrNull(rawEnvironment);
        if (!SUPPORTED_ENVIRONMENTS.contains(environment)) {
            throw ResponseException.internalServerError(
                    "Die FIT-Connect-Umgebung des Trigger-Knotens ist ungültig konfiguriert."
            );
        }
        return environment;
    }

    @Nullable
    private String normalizeEnvironmentOrNull(@Nullable String rawEnvironment) {
        var environment = StringUtils.toNullableTrimmedString(rawEnvironment);
        return environment == null ? FitConnectTriggerConfigV1.DEFAULT_ENVIRONMENT : environment.toUpperCase();
    }

    @Nonnull
    private String requireValue(@Nullable String rawValue,
                                @Nonnull String errorMessage) throws ResponseException {
        var value = StringUtils.toNullableTrimmedString(rawValue);
        if (value == null) {
            throw ResponseException.internalServerError(errorMessage);
        }
        return value;
    }

    @Nonnull
    private String resolveSecret(@Nullable String rawSecretKey,
                                 @Nonnull String description) throws ResponseException {
        var secretKeyValue = requireValue(rawSecretKey, description + " ist nicht konfiguriert.");
        final UUID secretKey;
        try {
            secretKey = UUID.fromString(secretKeyValue);
        } catch (IllegalArgumentException e) {
            throw ResponseException.internalServerError(description + " ist ungültig konfiguriert.", e);
        }

        var secret = secretService
                .retrieve(secretKey)
                .orElseThrow(() -> ResponseException.internalServerError(description + " wurde nicht gefunden."));
        try {
            var decryptedSecret = secretService.decrypt(secret);
            if (decryptedSecret.isBlank()) {
                throw ResponseException.internalServerError(description + " darf nicht leer sein.");
            }
            return decryptedSecret;
        } catch (ResponseException e) {
            throw e;
        } catch (Exception e) {
            throw ResponseException.internalServerError(description + " konnte nicht entschlüsselt werden.", e);
        }
    }

    @Nonnull
    private List<JWK> resolveDecryptionKeys(@Nullable List<FitConnectTriggerConfigV1.PrivateDecryptionKeyConfig> keyConfigs) throws ResponseException {
        if (keyConfigs == null || keyConfigs.isEmpty()) {
            throw ResponseException.internalServerError(
                    "Es ist kein privater Entschlüsselungsschlüssel für den FIT-Connect-Trigger konfiguriert."
            );
        }

        var keys = new ArrayList<JWK>(keyConfigs.size());
        for (var keyConfig : keyConfigs) {
            keys.add(resolveJwk(
                    keyConfig == null ? null : keyConfig.keyFile,
                    "Ein privater Entschlüsselungsschlüssel des FIT-Connect-Trigger-Knotens"
            ));
        }
        return keys;
    }

    @Nonnull
    private JWK resolveJwk(@Nullable StoragePathSelectorInputElementValue keyFile,
                           @Nonnull String description) throws ResponseException {
        if (keyFile == null || keyFile.getStorageProviderId() == null ||
                StringUtils.toNullableTrimmedString(keyFile.getPath()) == null) {
            throw ResponseException.internalServerError(description + " ist nicht konfiguriert.");
        }

        try (var content = storageService.getDocumentContent(keyFile.getStorageProviderId(), keyFile.getPath())) {
            var jwk = JWK.parse(new String(content.readAllBytes(), StandardCharsets.UTF_8));
            if (!(jwk instanceof RSAKey) || !jwk.isPrivate()) {
                throw ResponseException.internalServerError(description + " muss ein privater RSA-JWK sein.");
            }
            return jwk;
        } catch (ResponseException e) {
            throw e;
        } catch (IOException | ParseException e) {
            throw ResponseException.internalServerError(description + " konnte nicht als JWK gelesen werden.", e);
        }
    }

    private void validateSecretReference(@Nullable String rawSecretKey,
                                         @Nonnull String fieldId,
                                         @Nonnull String description,
                                         @Nonnull List<ValidationIssue> issues) {
        try {
            resolveSecret(rawSecretKey, description);
        } catch (ResponseException e) {
            issues.add(new ValidationIssue(fieldId, e.getMessage()));
        }
    }

    private void validateJwkReference(@Nullable StoragePathSelectorInputElementValue keyFile,
                                      @Nonnull String fieldId,
                                      @Nonnull String description,
                                      @Nonnull KeyOperation requiredOperation,
                                      @Nonnull List<ValidationIssue> issues) {
        try {
            var jwk = resolveJwk(keyFile, description);
            if (jwk.getKeyOperations() == null || !jwk.getKeyOperations().contains(requiredOperation)) {
                issues.add(new ValidationIssue(
                        fieldId,
                        description + " muss die Schlüsseloperation „" + requiredOperation.identifier() + "“ erlauben."
                ));
            }
            if (requiredOperation == KeyOperation.UNWRAP_KEY &&
                    StringUtils.toNullableTrimmedString(jwk.getKeyID()) == null) {
                issues.add(new ValidationIssue(fieldId, description + " benötigt eine Key-ID (kid)."));
            }
        } catch (ResponseException e) {
            issues.add(new ValidationIssue(fieldId, e.getMessage()));
        }
    }

    public record ValidationIssue(@Nonnull String fieldId, @Nonnull String message) {
    }
}
