package de.aivot.GoverBackend.form.entities.projections;

import java.time.LocalDateTime;

public interface FormEditorProjection {
    Integer getFormId();

    Integer getFormVersion();

    String getFullName();

    LocalDateTime getTimestamp();
}
