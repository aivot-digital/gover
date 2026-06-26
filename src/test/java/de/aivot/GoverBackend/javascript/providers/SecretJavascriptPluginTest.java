package de.aivot.GoverBackend.javascript.providers;

import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.javascript.services.JavascriptEngine;
import de.aivot.GoverBackend.plugins.core.v1.javascript.SecretJavascriptV1;
import de.aivot.GoverBackend.secrets.entities.SecretEntity;
import de.aivot.GoverBackend.secrets.services.SecretService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecretJavascriptPluginTest {
    private SecretService secretService;

    @BeforeEach
    void setUp() {
        secretService = mock(SecretService.class);
    }

    @Test
    void get() {
        try (var jsService = new JavascriptEngine(new SecretJavascriptV1(secretService))) {
            var secretKey = UUID.fromString("5f1512b5-9209-41d7-b12e-7cbbce6307c7");
            var secret = new SecretEntity();
            secret.setKey(secretKey);

            when(secretService.retrieve(secretKey))
                    .thenReturn(Optional.of(secret));
            when(secretService.decrypt(secret))
                    .thenReturn("decrypted-secret");

            var result = jsService.evaluateCode(new JavascriptCode().setCode("_secrets_v1.get('5f1512b5-9209-41d7-b12e-7cbbce6307c7');"));

            assertEquals("decrypted-secret", result.asString());
            verify(secretService).retrieve(secretKey);
            verify(secretService).decrypt(secret);
        } catch (Exception e) {
            fail(e);
        }
    }
}
