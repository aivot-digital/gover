package de.aivot.GoverBackend.submission.dtos;

import de.aivot.GoverBackend.enums.SubmissionStatus;
import de.aivot.GoverBackend.submission.entities.Submission;
import de.aivot.GoverBackend.submission.entities.SubmissionWithMembership;

import java.time.LocalDateTime;

public record SubmissionListResponseDTO(
        String id,
        Integer formId,
        SubmissionStatus status,
        String assigneeId,
        String fileNumber,
        Boolean isTestSubmission,
        Integer destinationId,
        Boolean destinationSuccess,
        LocalDateTime created
) {
    public static SubmissionListResponseDTO fromEntity(Submission submission) {
        return new SubmissionListResponseDTO(
                submission.getId(),
                submission.getFormId(),
                submission.getStatus(),
                submission.getAssigneeId(),
                submission.getFileNumber(),
                submission.getIsTestSubmission(),
                submission.getDestinationId(),
                submission.getDestinationSuccess(),
                submission.getCreated()
        );
    }

    public static SubmissionListResponseDTO fromEntity(SubmissionWithMembership submission) {
        return new SubmissionListResponseDTO(
                submission.getId(),
                submission.getFormId(),
                submission.getStatus(),
                submission.getAssigneeId(),
                submission.getFileNumber(),
                submission.getTestSubmission(),
                submission.getDestinationId(),
                submission.getDestinationSuccess(),
                submission.getCreated()
        );
    }
}
