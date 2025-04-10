package de.aivot.GoverBackend.submission.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.validator.constraints.Length;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;

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

    @Column(length = 255)
    private String contentType;

    @Column(length = 32)
    private String type;

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

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @JsonIgnore
    public MediaType getMediaType() {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    // endregion
}
