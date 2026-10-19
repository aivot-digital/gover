package de.aivot.prosuna.backend.plugins.core.v1.nodes.triggers.fitconnect;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/** Verifies the authenticity and freshness of incoming FIT-Connect callbacks. */
@Service
public class FitConnectTriggerCallbackAuthenticationServiceV1 {
    private static final String HMAC_ALGORITHM = "HmacSHA512";
    private static final long MAX_CALLBACK_AGE_SECONDS = 5 * 60;
    private static final String UNAUTHORIZED_MESSAGE = "Die FIT-Connect-Callback-Authentifizierung ist ungültig.";

    private final SecretService secretService;
    private final Clock clock;

    public FitConnectTriggerCallbackAuthenticationServiceV1(SecretService secretService, Clock clock) {
        this.secretService = secretService;
        this.clock = clock;
    }

    /**
     * Validates the FIT-Connect timestamp and HMAC before the callback is processed.
     *
     * <p>The HMAC input is the exact timestamp header, a period, and the unchanged HTTP request
     * body. FIT-Connect signs these values as UTF-8 using HMAC-SHA-512.</p>
     */
    public void authenticate(@Nullable String callbackSecretKey,
                             @Nullable String authenticationHeader,
                             @Nullable String timestampHeader,
                             @Nonnull byte[] rawBody) throws ResponseException {
        if (authenticationHeader == null || authenticationHeader.isBlank()
                || timestampHeader == null || timestampHeader.isBlank()) {
            throw unauthorized();
        }

        validateTimestamp(timestampHeader);

        final byte[] providedHmac;
        try {
            providedHmac = HexFormat.of().parseHex(authenticationHeader);
        } catch (IllegalArgumentException e) {
            throw unauthorized();
        }

        var callbackSecret = resolveCallbackSecret(callbackSecretKey);
        var expectedHmac = calculateHmac(timestampHeader, rawBody, callbackSecret);
        if (!MessageDigest.isEqual(providedHmac, expectedHmac)) {
            throw unauthorized();
        }
    }

    private void validateTimestamp(@Nonnull String timestampHeader) throws ResponseException {
        final Instant callbackTimestamp;
        try {
            callbackTimestamp = Instant.ofEpochSecond(Long.parseLong(timestampHeader));
        } catch (NumberFormatException | DateTimeException e) {
            throw unauthorized();
        }

        var oldestAcceptedTimestamp = clock.instant().minusSeconds(MAX_CALLBACK_AGE_SECONDS);
        if (callbackTimestamp.isBefore(oldestAcceptedTimestamp)) {
            throw unauthorized();
        }
    }

    @Nonnull
    private String resolveCallbackSecret(@Nullable String rawSecretKey) throws ResponseException {
        var normalizedSecretKey = StringUtils.toNullableTrimmedString(rawSecretKey);
        if (normalizedSecretKey == null) {
            throw ResponseException.internalServerError(
                    "Das Callback-Geheimnis des FIT-Connect-Trigger-Knotens ist nicht konfiguriert."
            );
        }

        final UUID secretKey;
        try {
            secretKey = UUID.fromString(normalizedSecretKey);
        } catch (IllegalArgumentException e) {
            throw ResponseException.internalServerError(
                    "Das Callback-Geheimnis des FIT-Connect-Trigger-Knotens ist ungültig konfiguriert."
            );
        }

        var secret = secretService
                .retrieve(secretKey)
                .orElseThrow(() -> ResponseException.internalServerError(
                        "Das Callback-Geheimnis des FIT-Connect-Trigger-Knotens wurde nicht gefunden."
                ));

        final String decryptedSecret;
        try {
            decryptedSecret = secretService.decrypt(secret);
        } catch (Exception e) {
            throw ResponseException.internalServerError(
                    "Das Callback-Geheimnis des FIT-Connect-Trigger-Knotens konnte nicht entschlüsselt werden.",
                    e
            );
        }

        if (decryptedSecret.isEmpty()) {
            throw ResponseException.internalServerError(
                    "Das Callback-Geheimnis des FIT-Connect-Trigger-Knotens darf nicht leer sein."
            );
        }
        return decryptedSecret;
    }

    @Nonnull
    private static byte[] calculateHmac(@Nonnull String timestampHeader,
                                        @Nonnull byte[] rawBody,
                                        @Nonnull String callbackSecret) throws ResponseException {
        try {
            var mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(callbackSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            mac.update(timestampHeader.getBytes(StandardCharsets.UTF_8));
            mac.update((byte) '.');
            return mac.doFinal(rawBody);
        } catch (GeneralSecurityException e) {
            throw ResponseException.internalServerError(
                    "Die FIT-Connect-Callback-Authentifizierung konnte nicht geprüft werden.",
                    e
            );
        }
    }

    @Nonnull
    private static ResponseException unauthorized() {
        return ResponseException.unauthorized(UNAUTHORIZED_MESSAGE);
    }
}
