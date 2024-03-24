package de.aivot.GoverBackend.models.entities;

import org.hibernate.validator.constraints.Length;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "submission_attachments")
public class SubmissionAttachment {
    @Id
    @Column(length = 36)
    private String id;

    @NotNull
    @Length(min = 36, max = 36)
    private String submissionId;

    @NotNull
    @Column(length = 36)
    private String filename;

    // region Getter & Setter

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    // endregion
}
