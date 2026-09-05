package de.aivot.prosuna.backend.plugins.core.v1.nodes.triggers.fitconnect;

import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.UUID;

/** Wire representation of a FIT-Connect callback for newly available submissions. */
public record FitConnectTriggerCallbackPayloadV1(
        @Nonnull String type,
        @Nonnull List<SubmissionReference> submissions
) {
    /** Identifiers required to retrieve one submission from FIT-Connect. */
    public record SubmissionReference(
            @Nonnull UUID destinationId,
            @Nonnull UUID submissionId,
            @Nonnull UUID caseId
    ) {
    }
}
