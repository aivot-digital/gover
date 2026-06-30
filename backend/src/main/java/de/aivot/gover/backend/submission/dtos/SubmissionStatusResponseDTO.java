package de.aivot.gover.backend.submission.dtos;

import java.util.UUID;

public record SubmissionStatusResponseDTO(
        UUID startedProcessAccessKey
) {
}
