package de.aivot.GoverBackend.submission.dtos;

import java.util.List;

public record SubmissionStatusResponseDTO(
        List<String> startedProcessAccessKeys
) {
}
