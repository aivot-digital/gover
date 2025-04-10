package de.aivot.GoverBackend.submission.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.submission.entities.SubmissionAttachment;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;

public class SubmissionAttachmentFilter implements Filter<SubmissionAttachment> {
    private String submissionId;
    private String contentType;
    private String type;

    public static SubmissionAttachmentFilter create() {
        return new SubmissionAttachmentFilter();
    }

    public SubmissionAttachmentFilter setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
        return this;
    }

    public SubmissionAttachmentFilter setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public SubmissionAttachmentFilter setType(String type) {
        this.type = type;
        return this;
    }

    @Nonnull
    @Override
    public Specification<SubmissionAttachment> build() {
        return SpecificationBuilder
                .create(SubmissionAttachment.class)
                .withEquals("submissionId", submissionId)
                .withEquals("contentType", contentType)
                .withEquals("type", type)
                .build();
    }
}
