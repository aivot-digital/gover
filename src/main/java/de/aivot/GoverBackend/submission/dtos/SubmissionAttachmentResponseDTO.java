package de.aivot.GoverBackend.submission.dtos;

import de.aivot.GoverBackend.submission.entities.SubmissionAttachment;

public record SubmissionAttachmentResponseDTO(
        String id,
        String submissionId,
        String filename,
        String contentType,
        String type
) {
    public static SubmissionAttachmentResponseDTO fromEntity(SubmissionAttachment entity) {
        return new SubmissionAttachmentResponseDTO(
                entity.getId(),
                entity.getSubmissionId(),
                entity.getFilename(),
                entity.getContentType(),
                entity.getType()
        );
    }
}
