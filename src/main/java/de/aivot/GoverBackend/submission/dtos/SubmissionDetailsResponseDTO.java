package de.aivot.GoverBackend.submission.dtos;

import de.aivot.GoverBackend.enums.SubmissionStatus;
import de.aivot.GoverBackend.submission.entities.Submission;
import de.aivot.GoverBackend.submission.entities.SubmissionWithMembership;

import java.time.LocalDateTime;
import java.util.Map;

public record SubmissionDetailsResponseDTO(
        String id,
        Integer formId,
        LocalDateTime created,
        LocalDateTime updated,
        LocalDateTime archived,
        SubmissionStatus status,
        String assigneeId,
        String fileNumber,
        Map<String, Object> customerInput,
        Integer destinationId,
        Boolean destinationSuccess,
        String destinationResult,
        LocalDateTime destinationTimestamp,
        Boolean isTestSubmission,
        Boolean copySent,
        Integer copyTries,
        Integer reviewScore,
        String paymentTransactionKey
) {
    public static SubmissionDetailsResponseDTO fromEntity(Submission submission) {
        return new SubmissionDetailsResponseDTO(
                submission.getId(),
                submission.getFormId(),
                submission.getCreated(),
                submission.getUpdated(),
                submission.getArchived(),
                submission.getStatus(),
                submission.getAssigneeId(),
                submission.getFileNumber(),
                submission.getCustomerInput(),
                submission.getDestinationId(),
                submission.getDestinationSuccess(),
                submission.getDestinationResult(),
                submission.getDestinationTimestamp(),
                submission.getIsTestSubmission(),
                submission.getCopySent(),
                submission.getCopyTries(),
                submission.getReviewScore(),
                submission.getPaymentTransactionKey()
        );
    }

    public static SubmissionDetailsResponseDTO fromEntity(SubmissionWithMembership submission) {
        return new SubmissionDetailsResponseDTO(
                submission.getId(),
                submission.getFormId(),
                submission.getCreated(),
                submission.getUpdated(),
                submission.getArchived(),
                submission.getStatus(),
                submission.getAssigneeId(),
                submission.getFileNumber(),
                submission.getCustomerInput(),
                submission.getDestinationId(),
                submission.getDestinationSuccess(),
                submission.getDestinationResult(),
                submission.getDestinationTimestamp(),
                submission.getTestSubmission(),
                submission.getCopySent(),
                submission.getCopyTries(),
                submission.getReviewScore(),
                submission.getPaymentTransactionKey()
        );
    }
}
