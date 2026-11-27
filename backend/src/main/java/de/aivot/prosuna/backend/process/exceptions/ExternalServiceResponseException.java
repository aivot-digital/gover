package de.aivot.prosuna.backend.process.exceptions;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Keeps a bounded response excerpt as the technical cause of a process error,
 * separate from its user-facing message. Do not include request credentials or headers.
 */
public final class ExternalServiceResponseException extends Exception {
    private static final int MAX_RESPONSE_CHARACTERS = 2_000;

    public ExternalServiceResponseException(@Nullable String responseDetails) {
        super(excerpt(responseDetails));
    }

    @Nonnull
    private static String excerpt(@Nullable String responseDetails) {
        if (responseDetails == null || responseDetails.isBlank()) {
            return "External service returned no diagnostic details.";
        }

        // Keep remote text on one line, including in server logs, and retain '%' literally.
        var excerpt = new StringBuilder();
        responseDetails.codePoints().limit(MAX_RESPONSE_CHARACTERS).forEach(codePoint ->
                excerpt.appendCodePoint(Character.isISOControl(codePoint)
                        || Character.isWhitespace(codePoint)
                        || Character.getType(codePoint) == Character.FORMAT ? ' ' : codePoint));
        var truncated = responseDetails.codePointCount(0, responseDetails.length()) > MAX_RESPONSE_CHARACTERS;
        return "External service response: " + excerpt.toString().strip() + (truncated ? " [truncated]" : "");
    }
}
