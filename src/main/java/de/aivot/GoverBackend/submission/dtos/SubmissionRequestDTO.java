package de.aivot.GoverBackend.submission.dtos;

import de.aivot.GoverBackend.lib.ReqeustDTO;
import de.aivot.GoverBackend.submission.entities.Submission;
import jakarta.validation.constraints.Max;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;

public record SubmissionRequestDTO(
        @Length(max = 96, message = "The file number must not exceed 96 characters")
        String fileNumber,

        @Length(max = 36, message = "The assignee ID must be at least 36 characters")
        String assigneeId,

        Boolean archived,

        Boolean canceled
) implements ReqeustDTO<Submission> {
    @Override
    public Submission toEntity() {
        var submission = new Submission();
        submission.setFileNumber(fileNumber);
        submission.setAssigneeId(assigneeId);

        if (Boolean.TRUE.equals(archived)) {
            submission.setArchived(LocalDateTime.now());
        }

        if (Boolean.TRUE.equals(canceled)) {
            submission.setDestinationId(null);
            submission.setDestinationTimestamp(null);
            submission.setDestinationResult(null);
            submission.setDestinationSuccess(null);
        }

        return submission;
    }
}
