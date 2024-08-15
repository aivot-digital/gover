package de.aivot.GoverBackend.models.entities;

import java.time.LocalDateTime;
import java.util.Map;

public interface SubmissionListProjection {
    String getId();

    Integer getFormId();

    LocalDateTime getCreated();

    String getAssigneeId();

    String getFileNumber();

    LocalDateTime getArchived();

    Integer getDestinationId();

    Boolean getDestinationSuccess();

    Boolean getIsTestSubmission();

    Boolean getCopySent();

    Integer getCopyTries();

    Integer getReviewScore();
}
