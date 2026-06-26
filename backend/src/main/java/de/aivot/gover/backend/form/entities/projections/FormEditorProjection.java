package de.aivot.gover.backend.form.entities.projections;

import java.time.Instant;

public interface FormEditorProjection {
    Integer getFormId();

    Integer getFormVersion();

    String getFullName();

    Instant getTimestamp();
}
