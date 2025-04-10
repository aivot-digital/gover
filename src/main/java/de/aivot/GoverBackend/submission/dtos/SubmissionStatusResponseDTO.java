package de.aivot.GoverBackend.submission.dtos;

import java.net.URI;

public record SubmissionStatusResponseDTO(
        String submissionId,
        String paymentProviderName,
        URI paymentProviderUrl,
        Boolean paymentDone,
        Boolean paymentFailed,
        Boolean accessExpired,
        Boolean copySent
) {
}
