package de.aivot.prosuna.backend.process.exceptions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ExternalServiceResponseExceptionTest {
    @Test
    void preservesDiagnosticTextAndLiteralFormatCharacters() {
        var response = "{\"error\":\"customerNumber is required; progress 100%, placeholder %s\"}";

        assertEquals("External service response: " + response,
                new ExternalServiceResponseException(response).getMessage());
    }

    @Test
    void keepsRemoteTextOnOneLineWithoutControlOrFormattingCharacters() {
        var exception = new ExternalServiceResponseException("Error\r\nfor\tfield\u0000name\u2028detail\u2029end\u202E!");

        assertEquals("External service response: Error  for field name detail end !", exception.getMessage());
    }

    @Test
    void truncatesLongResponsesWithoutSplittingUnicodeCharacters() {
        var response = "x".repeat(1_999) + "😀";
        assertEquals("External service response: " + response,
                new ExternalServiceResponseException(response).getMessage());

        var message = new ExternalServiceResponseException(response + "omitted").getMessage();
        assertEquals("External service response: " + response + " [truncated]", message);
        assertFalse(message.contains("omitted"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\r\n\t"})
    void handlesMissingDiagnosticDetails(String response) {
        assertEquals("External service returned no diagnostic details.",
                new ExternalServiceResponseException(response).getMessage());
    }
}
