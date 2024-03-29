package de.aivot.GoverBackend.models.entities;

import de.aivot.GoverBackend.converters.JsonObjectConverter;
import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "submissions")
public class Submission {
    @Id
    @Column(length = 36)
    private String id;

    @NotNull
    private Integer formId;

    @NotNull
    private LocalDateTime created;

    private String assigneeId;

    private String fileNumber;

    private LocalDateTime archived;

    @NotNull
    @Convert(converter = JsonObjectConverter.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> customerInput;

    private Integer destinationId;

    private Boolean destinationSuccess;

    private String destinationResult;

    private LocalDateTime destinationTimestamp;

    @NotNull
    private Boolean isTestSubmission;

    @NotNull
    @ColumnDefault("FALSE")
    private Boolean copySent;

    @NotNull
    @ColumnDefault("0")
    private Integer copyTries;

    private Integer reviewScore;

    @PrePersist
    public void prePersist() {
        created = LocalDateTime.now();
    }

    // region Getter & Setter

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getFormId() {
        return formId;
    }

    public void setFormId(Integer formId) {
        this.formId = formId;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public String getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(String assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getFileNumber() {
        return fileNumber;
    }

    public void setFileNumber(String fileNumber) {
        this.fileNumber = fileNumber;
    }

    public LocalDateTime getArchived() {
        return archived;
    }

    public void setArchived(LocalDateTime archived) {
        this.archived = archived;
    }

    public Map<String, Object> getCustomerInput() {
        return customerInput;
    }

    public void setCustomerInput(Map<String, Object> customerInput) {
        this.customerInput = customerInput;
    }

    public Integer getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Integer destinationId) {
        this.destinationId = destinationId;
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

    public Boolean getCopySent() {
        return copySent;
    }

    public void setCopySent(Boolean copySent) {
        this.copySent = copySent;
    }

    public Integer getCopyTries() {
        return copyTries;
    }

    public void setCopyTries(Integer copyTries) {
        this.copyTries = copyTries;
    }

    public Integer getReviewScore() {
        return reviewScore;
    }

    public void setReviewScore(Integer reviewScore) {
        this.reviewScore = reviewScore;
    }

    public String getDestinationResult() {
        return destinationResult;
    }

    public void setDestinationResult(String destinationResult) {
        this.destinationResult = destinationResult;
    }

    public LocalDateTime getDestinationTimestamp() {
        return destinationTimestamp;
    }

    public void setDestinationTimestamp(LocalDateTime destinationTimestamp) {
        this.destinationTimestamp = destinationTimestamp;
    }

    // endregion
}
