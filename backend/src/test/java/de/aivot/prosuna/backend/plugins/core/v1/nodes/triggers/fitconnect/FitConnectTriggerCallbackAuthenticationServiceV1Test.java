package de.aivot.prosuna.backend.plugins.core.v1.nodes.triggers.fitconnect;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.secrets.entities.SecretEntity;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FitConnectTriggerCallbackAuthenticationServiceV1Test {
    private static final Instant NOW = Instant.parse("2026-09-02T10:00:00Z");
    private static final String CALLBACK_SECRET = "callback-secret";
    private static final byte[] RAW_BODY = "{\n  \"type\": \"test\"\n}".getBytes(StandardCharsets.UTF_8);

    private SecretService secretService;
    private UUID secretKey;
    private SecretEntity secretEntity;
    private FitConnectTriggerCallbackAuthenticationServiceV1 authenticationService;

    @BeforeEach
    void setUp() throws Exception {
        secretService = mock(SecretService.class);
        secretKey = UUID.randomUUID();
        secretEntity = mock(SecretEntity.class);
        when(secretService.retrieve(secretKey)).thenReturn(Optional.of(secretEntity));
        when(secretService.decrypt(secretEntity)).thenReturn(CALLBACK_SECRET);
        authenticationService = new FitConnectTriggerCallbackAuthenticationServiceV1(
                secretService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void acceptsValidHmacWithoutNormalizingTheRawBody() throws Exception {
        var timestamp = Long.toString(NOW.getEpochSecond());
        var authentication = sign(timestamp, RAW_BODY, CALLBACK_SECRET).toUpperCase();

        assertDoesNotThrow(() -> authenticationService.authenticate(
                secretKey.toString(),
                authentication,
                timestamp,
                RAW_BODY
        ));

        verify(secretService).retrieve(secretKey);
        verify(secretService).decrypt(secretEntity);
    }

    @Test
    void acceptsTimestampAtFiveMinuteBoundary() throws Exception {
        var timestamp = Long.toString(NOW.minusSeconds(300).getEpochSecond());

        assertDoesNotThrow(() -> authenticationService.authenticate(
                secretKey.toString(),
                sign(timestamp, RAW_BODY, CALLBACK_SECRET),
                timestamp,
                RAW_BODY
        ));
    }

    @Test
    void acceptsFutureTimestampLikeTheFitConnectContract() throws Exception {
        var timestamp = Long.toString(NOW.plusSeconds(60).getEpochSecond());

        assertDoesNotThrow(() -> authenticationService.authenticate(
                secretKey.toString(),
                sign(timestamp, RAW_BODY, CALLBACK_SECRET),
                timestamp,
                RAW_BODY
        ));
    }

    @Test
    void rejectsExpiredTimestampBeforeLoadingTheSecret() throws Exception {
        var timestamp = Long.toString(NOW.minusSeconds(301).getEpochSecond());

        var exception = assertThrows(ResponseException.class, () -> authenticationService.authenticate(
                secretKey.toString(),
                sign(timestamp, RAW_BODY, CALLBACK_SECRET),
                timestamp,
                RAW_BODY
        ));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        verifyNoInteractions(secretService);
    }

    @Test
    void rejectsMissingOrMalformedHeadersWithoutLoadingTheSecret() {
        assertUnauthorized(() -> authenticationService.authenticate(secretKey.toString(), null, "1", RAW_BODY));
        assertUnauthorized(() -> authenticationService.authenticate(secretKey.toString(), "00", null, RAW_BODY));
        assertUnauthorized(() -> authenticationService.authenticate(secretKey.toString(), "00", "not-a-timestamp", RAW_BODY));
        assertUnauthorized(() -> authenticationService.authenticate(secretKey.toString(), "not-hex", Long.toString(NOW.getEpochSecond()), RAW_BODY));

        verifyNoInteractions(secretService);
    }

    @Test
    void rejectsHmacWhenTheRawBodyChanges() throws Exception {
        var timestamp = Long.toString(NOW.getEpochSecond());
        var authentication = sign(timestamp, RAW_BODY, CALLBACK_SECRET);
        var changedBody = "{\"type\":\"test\"}".getBytes(StandardCharsets.UTF_8);

        assertUnauthorized(() -> authenticationService.authenticate(
                secretKey.toString(),
                authentication,
                timestamp,
                changedBody
        ));
    }

    @Test
    void rejectsUnknownSecretAsInternalServerError() {
        var missingKey = UUID.randomUUID();
        when(secretService.retrieve(missingKey)).thenReturn(Optional.empty());
        var timestamp = Long.toString(NOW.getEpochSecond());

        var exception = assertThrows(ResponseException.class, () -> authenticationService.authenticate(
                missingKey.toString(),
                sign(timestamp, RAW_BODY, CALLBACK_SECRET),
                timestamp,
                RAW_BODY
        ));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
    }

    @Test
    void wrapsSecretDecryptionFailureAsInternalServerError() throws Exception {
        var cause = new Exception("decryption failed");
        when(secretService.decrypt(secretEntity)).thenThrow(cause);
        var timestamp = Long.toString(NOW.getEpochSecond());

        var exception = assertThrows(ResponseException.class, () -> authenticationService.authenticate(
                secretKey.toString(),
                sign(timestamp, RAW_BODY, CALLBACK_SECRET),
                timestamp,
                RAW_BODY
        ));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void rejectsInvalidSecretKeyAsInternalServerError() throws Exception {
        var timestamp = Long.toString(NOW.getEpochSecond());

        var exception = assertThrows(ResponseException.class, () -> authenticationService.authenticate(
                "not-a-uuid",
                sign(timestamp, RAW_BODY, CALLBACK_SECRET),
                timestamp,
                RAW_BODY
        ));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        verify(secretService, never()).retrieve(secretKey);
    }

    private static void assertUnauthorized(ThrowingCall call) {
        var exception = assertThrows(ResponseException.class, call::run);
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Die FIT-Connect-Callback-Authentifizierung ist ungültig.", exception.getMessage());
    }

    private static String sign(String timestamp, byte[] rawBody, String secret) throws Exception {
        var mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        mac.update(timestamp.getBytes(StandardCharsets.UTF_8));
        mac.update((byte) '.');
        return HexFormat.of().formatHex(mac.doFinal(rawBody));
    }

    @FunctionalInterface
    private interface ThrowingCall {
        void run() throws Exception;
    }
}
