package de.aivot.GoverBackend.submission.filters;

import de.aivot.GoverBackend.enums.SubmissionStatus;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.submission.entities.Submission;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

public class SubmissionFilter implements Filter<Submission> {
    private Integer formId;
    private String assigneeId;
    private SubmissionStatus status;
    private String fileNumber;
    private Integer destinationId;
    private Boolean notArchived;
    private Boolean notPending;
    private Boolean notTestSubmission;
    private String paymentTransactionKey;

    public static SubmissionFilter create() {
        return new SubmissionFilter();
    }

    @Override
    public Specification<Submission> build() {
        var spec = SpecificationBuilder
                .create(Submission.class)
                .withEquals("formId", formId)
                .withEquals("assigneeId", assigneeId)
                .withEquals("status", status)
                .withEquals("fileNumber", fileNumber)
                .withEquals("destinationId", destinationId)
                .withEquals("paymentTransactionKey", paymentTransactionKey);

        if (Boolean.TRUE.equals(notTestSubmission)) {
            spec = spec.withEquals("isTestSubmission", false);
        }

        if (Boolean.TRUE.equals(notArchived)) {
            spec = spec.withNotEquals("status", SubmissionStatus.Archived);
        }

        if (Boolean.TRUE.equals(notPending)) {
            spec = spec.withNotEquals("status", SubmissionStatus.Pending);
        }

        return spec.build();
    }

    public SubmissionFilter setFormId(Integer formId) {
        this.formId = formId;
        return this;
    }

    public SubmissionFilter setAssigneeId(String assigneeId) {
        this.assigneeId = assigneeId;
        return this;
    }

    public SubmissionFilter setStatus(SubmissionStatus status) {
        this.status = status;
        return this;
    }

    public SubmissionFilter setFileNumber(String fileNumber) {
        this.fileNumber = fileNumber;
        return this;
    }

    public SubmissionFilter setDestinationId(Integer destinationId) {
        this.destinationId = destinationId;
        return this;
    }

    public SubmissionFilter setNotTestSubmission(Boolean notTestSubmission) {
        this.notTestSubmission = notTestSubmission;
        return this;
    }

    public SubmissionFilter setNotArchived(Boolean notArchived) {
        this.notArchived = notArchived;
        return this;
    }

    public SubmissionFilter setNotPending(Boolean notPending) {
        this.notPending = notPending;
        return this;
    }

    public String getPaymentTransactionKey() {
        return paymentTransactionKey;
    }

    public SubmissionFilter setPaymentTransactionKey(String paymentTransactionKey) {
        this.paymentTransactionKey = paymentTransactionKey;
        return this;
    }
}
