package de.aivot.GoverBackend.submission.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.core.converters.JsonObjectConverter;
import de.aivot.GoverBackend.enums.SubmissionStatus;
import de.aivot.GoverBackend.form.entities.Form;
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

    @NotNull
    private LocalDateTime updated;

    @NotNull
    @Column(columnDefinition = "int4")
    private SubmissionStatus status;

    private String assigneeId;

    private String fileNumber;

    private LocalDateTime archived;

    @NotNull
    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonObjectConverter.class)
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

    @Column(length = 36)
    private String paymentTransactionKey;

    @PrePersist
    public void prePersist() {
        created = LocalDateTime.now();
        updated = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updated = LocalDateTime.now();
    }

    // region Getter & Setter

    public String getId() {
        return id;
    }

    public Submission setId(String id) {
        this.id = id;
        return this;
    }

    public Integer getFormId() {
        return formId;
    }

    public Submission setFormId(Integer formId) {
        this.formId = formId;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public Submission setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public Submission setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public Submission setStatus(SubmissionStatus status) {
        this.status = status;
        return this;
    }

    public String getAssigneeId() {
        return assigneeId;
    }

    public Submission setAssigneeId(String assigneeId) {
        this.assigneeId = assigneeId;
        return this;
    }

    public String getFileNumber() {
        return fileNumber;
    }

    public Submission setFileNumber(String fileNumber) {
        this.fileNumber = fileNumber;
        return this;
    }

    public LocalDateTime getArchived() {
        return archived;
    }

    public Submission setArchived(LocalDateTime archived) {
        this.archived = archived;
        return this;
    }

    public Map<String, Object> getCustomerInput() {
        return customerInput;
    }

    public Submission setCustomerInput(Map<String, Object> customerInput) {
        this.customerInput = customerInput;
        return this;
    }

    public Integer getDestinationId() {
        return destinationId;
    }

    public Submission setDestinationId(Integer destinationId) {
        this.destinationId = destinationId;
        return this;
    }

    public Boolean getDestinationSuccess() {
        return destinationSuccess;
    }

    public Submission setDestinationSuccess(Boolean destinationSuccess) {
        this.destinationSuccess = destinationSuccess;
        return this;
    }

    public String getDestinationResult() {
        return destinationResult;
    }

    public Submission setDestinationResult(String destinationResult) {
        this.destinationResult = destinationResult;
        return this;
    }

    public LocalDateTime getDestinationTimestamp() {
        return destinationTimestamp;
    }

    public Submission setDestinationTimestamp(LocalDateTime destinationTimestamp) {
        this.destinationTimestamp = destinationTimestamp;
        return this;
    }

    public Boolean getIsTestSubmission() {
        return isTestSubmission;
    }

    public Submission setIsTestSubmission(Boolean testSubmission) {
        isTestSubmission = testSubmission;
        return this;
    }

    public Boolean getCopySent() {
        return copySent;
    }

    public Submission setCopySent(Boolean copySent) {
        this.copySent = copySent;
        return this;
    }

    public Integer getCopyTries() {
        return copyTries;
    }

    public Submission setCopyTries(Integer copyTries) {
        this.copyTries = copyTries;
        return this;
    }

    public Integer getReviewScore() {
        return reviewScore;
    }

    public Submission setReviewScore(Integer reviewScore) {
        this.reviewScore = reviewScore;
        return this;
    }

    public String getPaymentTransactionKey() {
        return paymentTransactionKey;
    }

    public Submission setPaymentTransactionKey(String paymentTransactionKey) {
        this.paymentTransactionKey = paymentTransactionKey;
        return this;
    }


    // endregion

    @JsonIgnore
    public boolean hasExternalAccessExpired(Form form) {
        int accessHours = form.getCustomerAccessHours() != null ? form.getCustomerAccessHours() : 4;

        LocalDateTime expirationTimestamp = created.plusHours(accessHours);
        LocalDateTime currentTimestamp = LocalDateTime.now();

        return currentTimestamp.isAfter(expirationTimestamp);
    }
}
