package de.aivot.GoverBackend.models.entities;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "submission_attachments")
public class SubmissionAttachment {
    @Id
    @Column(length = 36)
    private String id;

    @NotNull
    @ManyToOne
    private Submission submission;

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

    public Submission getSubmission() {
        return submission;
    }

    public void setSubmission(Submission submission) {
        this.submission = submission;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }


    // endregion
}
