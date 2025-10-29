package de.aivot.GoverBackend.form.entities;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class FormEditorEntity {
    private Integer formId;
    private Integer formVersion;
    private String fullName;
    private LocalDateTime timestamp;

    public FormEditorEntity(Integer formId, Integer formVersion, String fullName, Timestamp timestamp) {
        this.formId = formId;
        this.formVersion = formVersion;
        this.fullName = fullName;
        this.timestamp = timestamp.toLocalDateTime();
    }

    public Integer getFormId() {
        return formId;
    }

    public void setFormId(Integer formId) {
        this.formId = formId;
    }

    public Integer getFormVersion() {
        return formVersion;
    }

    public void setFormVersion(Integer formVersion) {
        this.formVersion = formVersion;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
