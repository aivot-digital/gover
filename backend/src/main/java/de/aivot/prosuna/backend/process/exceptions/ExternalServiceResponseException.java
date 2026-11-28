package de.aivot.prosuna.backend.process.exceptions;

import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nullable;

/**
 * Keeps a bounded response excerpt as the technical cause of a process error,
 * separate from its user-facing message. Do not include request credentials or headers.
 */
public final class ExternalServiceResponseException extends Exception {
    public ExternalServiceResponseException(@Nullable String responseDetails) {
        super(responseDetails == null || responseDetails.isBlank()
                ? "External service returned no diagnostic details."
                : "External service response: " + StringUtils.diagnosticExcerpt(responseDetails));
    }
}
