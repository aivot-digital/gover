package de.aivot.GoverBackend.submission.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.core.converters.AuthoredElementValuesConverter;
import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.enums.SubmissionStatus;
import de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsEntity;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "submissions")
public class Submission {
    @Id
    @Column(length = 36)
    private String id;

    @Nonnull
    private Integer formId;

    @Nonnull
    private Integer formVersion;

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
    @Convert(converter = AuthoredElementValuesConverter.class)
    @Column(columnDefinition = "jsonb")
    private AuthoredElementValues customerInput;

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

    // Equals & HashCode

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        Submission that = (Submission) object;
        return Objects.equals(id, that.id) && formId.equals(that.formId) && formVersion.equals(that.formVersion) && Objects.equals(created, that.created) && Objects.equals(updated, that.updated) && status == that.status && Objects.equals(assigneeId, that.assigneeId) && Objects.equals(fileNumber, that.fileNumber) && Objects.equals(archived, that.archived) && Objects.equals(customerInput, that.customerInput) && Objects.equals(destinationId, that.destinationId) && Objects.equals(destinationSuccess, that.destinationSuccess) && Objects.equals(destinationResult, that.destinationResult) && Objects.equals(destinationTimestamp, that.destinationTimestamp) && Objects.equals(isTestSubmission, that.isTestSubmission) && Objects.equals(copySent, that.copySent) && Objects.equals(copyTries, that.copyTries) && Objects.equals(reviewScore, that.reviewScore) && Objects.equals(paymentTransactionKey, that.paymentTransactionKey);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + formId.hashCode();
        result = 31 * result + formVersion.hashCode();
        result = 31 * result + Objects.hashCode(created);
        result = 31 * result + Objects.hashCode(updated);
        result = 31 * result + Objects.hashCode(status);
        result = 31 * result + Objects.hashCode(assigneeId);
        result = 31 * result + Objects.hashCode(fileNumber);
        result = 31 * result + Objects.hashCode(archived);
        result = 31 * result + Objects.hashCode(customerInput);
        result = 31 * result + Objects.hashCode(destinationId);
        result = 31 * result + Objects.hashCode(destinationSuccess);
        result = 31 * result + Objects.hashCode(destinationResult);
        result = 31 * result + Objects.hashCode(destinationTimestamp);
        result = 31 * result + Objects.hashCode(isTestSubmission);
        result = 31 * result + Objects.hashCode(copySent);
        result = 31 * result + Objects.hashCode(copyTries);
        result = 31 * result + Objects.hashCode(reviewScore);
        result = 31 * result + Objects.hashCode(paymentTransactionKey);
        return result;
    }

    // endregion

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

    public AuthoredElementValues getCustomerInput() {
        return customerInput;
    }

    public Submission setCustomerInput(AuthoredElementValues customerInput) {
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
    public boolean hasExternalAccessExpired(VFormVersionWithDetailsEntity form) {
        int accessHours = form.getCustomerAccessHours() != null ? form.getCustomerAccessHours() : 4;

        LocalDateTime expirationTimestamp = created.plusHours(accessHours);
        LocalDateTime currentTimestamp = LocalDateTime.now();

        return currentTimestamp.isAfter(expirationTimestamp);
    }

    @Nonnull
    public Integer getFormVersion() {
        return formVersion;
    }

    public Submission setFormVersion(@Nonnull Integer formVersion) {
        this.formVersion = formVersion;
        return this;
    }
}
