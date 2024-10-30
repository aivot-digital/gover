package de.aivot.GoverBackend.models.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.converters.JsonObjectConverter;
import de.aivot.GoverBackend.enums.PaymentProvider;
import de.aivot.GoverBackend.enums.SubmissionStatus;
import de.aivot.GoverBackend.enums.XBezahldienstStatus;
import de.aivot.GoverBackend.models.xbezahldienst.XBezahldienstePaymentInformation;
import de.aivot.GoverBackend.models.xbezahldienst.XBezahldienstePaymentRequest;
import de.aivot.GoverBackend.models.xbezahldienst.XBezahldienstePaymentTransaction;
import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;

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

    private String paymentOriginatorId;

    private String paymentEndpointId;

    private PaymentProvider paymentProvider;

    @JdbcTypeCode(SqlTypes.JSON)
    private XBezahldienstePaymentRequest paymentRequest;

    @JdbcTypeCode(SqlTypes.JSON)
    private XBezahldienstePaymentInformation paymentInformation;

    private String paymentError;

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

    public SubmissionStatus getStatus() {
        return status;
    }

    public void setStatus(SubmissionStatus status) {
        this.status = status;
    }

    public XBezahldienstePaymentRequest getPaymentRequest() {
        return paymentRequest;
    }

    public void setPaymentRequest(XBezahldienstePaymentRequest paymentRequest) {
        this.paymentRequest = paymentRequest;
    }

    public XBezahldienstePaymentInformation getPaymentInformation() {
        return paymentInformation;
    }

    public void setPaymentInformation(XBezahldienstePaymentInformation paymentInformation) {
        this.paymentInformation = paymentInformation;
    }

    public String getPaymentError() {
        return paymentError;
    }

    public void setPaymentError(String paymentError) {
        this.paymentError = paymentError;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    public PaymentProvider getPaymentProvider() {
        return paymentProvider;
    }

    public void setPaymentProvider(PaymentProvider paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    public String getPaymentEndpointId() {
        return paymentEndpointId;
    }

    public void setPaymentEndpointId(String paymentEndpointId) {
        this.paymentEndpointId = paymentEndpointId;
    }

    public String getPaymentOriginatorId() {
        return paymentOriginatorId;
    }

    public void setPaymentOriginatorId(String paymentOriginatorId) {
        this.paymentOriginatorId = paymentOriginatorId;
    }

    @JsonIgnore
    public boolean hasExternalAccessExpired(Form form) {
        int accessHours = form.getCustomerAccessHours() != null ? form.getCustomerAccessHours() : 4;

        LocalDateTime expirationTimestamp = created.plusHours(accessHours);
        LocalDateTime currentTimestamp = LocalDateTime.now();

        return currentTimestamp.isAfter(expirationTimestamp);
    }

    // endregion
}
