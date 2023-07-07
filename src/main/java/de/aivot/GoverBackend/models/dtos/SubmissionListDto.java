package de.aivot.GoverBackend.models.dtos;

import de.aivot.GoverBackend.models.entities.Submission;

import java.time.LocalDateTime;


public class SubmissionListDto {
    private String id;

    private Integer application;

    private LocalDateTime created;

    private LocalDateTime archived;

    private Integer assignee;

    private String fileNumber;

    private Integer destination;

    private Boolean destinationSuccess;

    private Boolean isTestSubmission;

    public SubmissionListDto() {
    }

    public SubmissionListDto(Submission submission) {
        id = submission.getId();
        application = submission.getApplication().getId();
        created = submission.getCreated();
        if (submission.getAssignee() != null) {
            assignee = submission.getAssignee().getId();
        }
        archived = submission.getArchived();
        fileNumber = submission.getFileNumber();
        if (submission.getDestination() != null) {
            destination = submission.getDestination().getId();
        }
        destinationSuccess = submission.getDestinationSuccess();
        isTestSubmission = submission.getIsTestSubmission();
    }

    // region Getters & Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getApplication() {
        return application;
    }

    public void setApplication(Integer application) {
        this.application = application;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getArchived() {
        return archived;
    }

    public void setArchived(LocalDateTime archived) {
        this.archived = archived;
    }

    public Integer getAssignee() {
        return assignee;
    }

    public void setAssignee(Integer assignee) {
        this.assignee = assignee;
    }

    public String getFileNumber() {
        return fileNumber;
    }

    public void setFileNumber(String fileNumber) {
        this.fileNumber = fileNumber;
    }

    public Integer getDestination() {
        return destination;
    }

    public void setDestination(Integer destination) {
        this.destination = destination;
    }

    public Boolean getDestinationSuccess() {
        return destinationSuccess;
    }

    public void setDestinationSuccess(Boolean destinationSuccess) {
        this.destinationSuccess = destinationSuccess;
    }

    public Boolean getIsTestSubmission() {
        return isTestSubmission;
    }

    public void setIsTestSubmission(Boolean testSubmission) {
        isTestSubmission = testSubmission;
    }

// endregion
}
