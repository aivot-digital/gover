package de.aivot.prosuna.backend.plugins.core.v1.nodes.triggers.fitconnect;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyOperation;
import com.nimbusds.jose.jwk.RSAKey;
import de.aivot.prosuna.backend.asset.services.AssetContentResolverService;
import de.aivot.prosuna.backend.elements.enums.AssetVisibility;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import de.aivot.prosuna.backend.utils.StringUtils;
import dev.fitko.fitconnect.rest.client.config.FitConnectEnvironment;
import dev.fitko.fitconnect.sdk.FitConnectSdk;
import dev.fitko.fitconnect.sdk.clients.Organisation;
import dev.fitko.fitconnect.sdk.config.credentials.DestinationKeys;
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

/** Builds FIT-Connect organisation clients from a validated trigger configuration. */
@Service
public class FitConnectTriggerOrganisationFactoryV1 {
    private static final Set<String> SUPPORTED_ENVIRONMENTS = Set.of("TEST", "STAGE", "PROD");

    private final SecretService secretService;
    private final AssetContentResolverService assetContentResolverService;

    public FitConnectTriggerOrganisationFactoryV1(SecretService secretService,
                                                   AssetContentResolverService assetContentResolverService) {
        this.secretService = secretService;
        this.assetContentResolverService = assetContentResolverService;
    }

    @Nonnull
    public Organisation create(@Nonnull FitConnectTriggerConfigV1 config) throws ResponseException {
        var environment = resolveEnvironment(config.environment);
        var destinationId = resolveDestinationId(config.destinationId);
        var clientId = requireValue(
                config.subscriberClientId,
                "Hinterlegen Sie die Client-ID des FIT-Connect-Empfängers."
        );
        var clientSecret = resolveSecret(
                config.subscriberClientSecret,
                "Client-Secret"
        );
        var signingKey = resolveJwk(
                config.privateSigningKey,
                "Der ausgewählte private Signaturschlüssel"
        );
        var decryptionKeys = resolveDecryptionKeys(config.privateDecryptionKeys);

        try {
            return FitConnectSdk
                    .fromConfigBuilder()
                    .credentials(clientId, clientSecret)
                    .environment(environment)
                    .build()
                    .organisation(destinationId, new DestinationKeys(decryptionKeys, signingKey));
        } catch (Exception e) {
            throw ResponseException.internalServerErrorWithDetails(
                    e,
                    "Der FIT-Connect-Zugang konnte nicht vorbereitet werden.",
                    null
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
                    "Wählen Sie eine unterstützte FIT-Connect-Umgebung: TEST, STAGE oder PROD."
            ));
        }
        if (StringUtils.toNullableTrimmedString(config.subscriberClientId) == null) {
            issues.add(new ValidationIssue(
                    FitConnectTriggerConfigV1.SUBSCRIBER_CLIENT_ID_CONFIG_KEY,
                    "Hinterlegen Sie die Client-ID des FIT-Connect-Empfängers."
            ));
        }

        validateSecretReference(
                config.subscriberClientSecret,
                FitConnectTriggerConfigV1.SUBSCRIBER_CLIENT_SECRET_CONFIG_KEY,
                "Client-Secret",
                issues
        );
        validateSecretReference(
                config.callbackSecret,
                FitConnectTriggerConfigV1.CALLBACK_SECRET_KEY,
                "Callback-Secret",
                issues
        );
        validateJwkReference(
                config.privateSigningKey,
                FitConnectTriggerConfigV1.PRIVATE_SIGNING_KEY_CONFIG_KEY,
                "Der ausgewählte private Signaturschlüssel",
                KeyOperation.SIGN,
                issues
        );

        if (config.privateDecryptionKeys == null || config.privateDecryptionKeys.isEmpty()) {
            issues.add(new ValidationIssue(
                    FitConnectTriggerConfigV1.PRIVATE_DECRYPTION_KEYS_CONFIG_KEY,
                    "Hinterlegen Sie mindestens einen privaten Entschlüsselungsschlüssel."
            ));
        } else {
            for (var keyConfig : config.privateDecryptionKeys) {
                validateJwkReference(
                        keyConfig == null ? null : keyConfig.keyFile,
                        FitConnectTriggerConfigV1.PRIVATE_DECRYPTION_KEYS_CONFIG_KEY,
                        "Der ausgewählte private Entschlüsselungsschlüssel",
                        KeyOperation.UNWRAP_KEY,
                        issues
                );
            }
        }

        return issues;
    }

    @Nonnull
    private FitConnectEnvironment resolveEnvironment(@Nullable String rawEnvironment) throws ResponseException {
        var environment = normalizeEnvironmentOrNull(rawEnvironment);
        if (!SUPPORTED_ENVIRONMENTS.contains(environment)) {
            throw ResponseException.internalServerError(
                    "Wählen Sie eine unterstützte FIT-Connect-Umgebung: TEST, STAGE oder PROD."
            );
        }
        return switch (environment) {
            case "TEST" -> FitConnectEnvironment.TEST;
            case "STAGE" -> FitConnectEnvironment.STAGE;
            case "PROD" -> FitConnectEnvironment.PROD;
            default -> throw ResponseException.internalServerError(
                    "Wählen Sie eine unterstützte FIT-Connect-Umgebung: TEST, STAGE oder PROD."
            );
        };
    }

    @Nonnull
    private UUID resolveDestinationId(@Nullable String rawDestinationId) throws ResponseException {
        var destinationId = requireValue(
                rawDestinationId,
                "Hinterlegen Sie die Zustellpunkt-ID aus FIT-Connect."
        );
        try {
            return UUID.fromString(destinationId);
        } catch (IllegalArgumentException e) {
            throw ResponseException.internalServerErrorWithDetails(
                    e,
                    "Die Zustellpunkt-ID muss eine gültige UUID sein.",
                    null
            );
        }
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
        var secretKeyValue = requireValue(rawSecretKey, "Wählen Sie ein Geheimnis für „" + description + "“ aus.");
        final UUID secretKey;
        try {
            secretKey = UUID.fromString(secretKeyValue);
        } catch (IllegalArgumentException e) {
            throw ResponseException.internalServerErrorWithDetails(e, "Das ausgewählte " + description + " ist ungültig.", null);
        }

        var secret = secretService
                .retrieve(secretKey)
                .orElseThrow(() -> ResponseException.internalServerError("Das ausgewählte " + description + " wurde nicht gefunden."));
        try {
            var decryptedSecret = secretService.decrypt(secret);
            if (decryptedSecret.isBlank()) {
                throw ResponseException.internalServerError("Das ausgewählte " + description + " darf nicht leer sein.");
            }
            return decryptedSecret;
        } catch (ResponseException e) {
            throw e;
        } catch (Exception e) {
            throw ResponseException.internalServerErrorWithDetails(e, "Das ausgewählte " + description + " konnte nicht entschlüsselt werden.", null);
        }
    }

    @Nonnull
    private List<JWK> resolveDecryptionKeys(@Nullable List<FitConnectTriggerConfigV1.PrivateDecryptionKeyConfig> keyConfigs) throws ResponseException {
        if (keyConfigs == null || keyConfigs.isEmpty()) {
            throw ResponseException.internalServerError(
                    "Hinterlegen Sie mindestens einen privaten Entschlüsselungsschlüssel."
            );
        }

        var keys = new ArrayList<JWK>(keyConfigs.size());
        for (var keyConfig : keyConfigs) {
            keys.add(resolveJwk(
                    keyConfig == null ? null : keyConfig.keyFile,
                    "Der ausgewählte private Entschlüsselungsschlüssel"
            ));
        }
        return keys;
    }

    @Nonnull
    private JWK resolveJwk(@Nullable String assetKey,
                           @Nonnull String description) throws ResponseException {
        var resolvedAssetKey = requireValue(assetKey, description + " ist nicht konfiguriert.");
        try {
            var content = assetContentResolverService.resolveContent(
                    resolvedAssetKey,
                    AssetVisibility.Private,
                    description
            );
            var jwk = JWK.parse(new String(content, StandardCharsets.UTF_8));
            if (!(jwk instanceof RSAKey) || !jwk.isPrivate()) {
                throw ResponseException.internalServerError(description + " muss ein privater RSA-JWK sein.");
            }
            return jwk;
        } catch (ResponseException e) {
            throw new ResponseException(e.getStatus(), e.getTitle(), null, e);
        } catch (ParseException e) {
            throw ResponseException.internalServerErrorWithDetails(e, description + " konnte nicht als JWK gelesen werden.", null);
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

    private void validateJwkReference(@Nullable String assetKey,
                                      @Nonnull String fieldId,
                                      @Nonnull String description,
                                      @Nonnull KeyOperation requiredOperation,
                                      @Nonnull List<ValidationIssue> issues) {
        try {
            var jwk = resolveJwk(assetKey, description);
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
