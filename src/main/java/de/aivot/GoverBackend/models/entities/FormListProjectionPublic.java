package de.aivot.GoverBackend.models.entities;

import java.time.LocalDateTime;

public interface FormListProjectionPublic {
    Integer getId();
    String getSlug();
    String getVersion();
    String getTitle();
    Integer getLegalSupportDepartmentId();
    Integer getTechnicalSupportDepartmentId();
    Integer getImprintDepartmentId();
    Integer getPrivacyDepartmentId();
    Integer getAccessibilityDepartmentId();
    Integer getDevelopingDepartmentId();
    Integer getManagingDepartmentId();
    Integer getResponsibleDepartmentId();
    Integer getThemeId();
    LocalDateTime getUpdated();
    Boolean getBundIdEnabled();
    Boolean getBayernIdEnabled();
    Boolean getMukEnabled();
    Boolean getShIdEnabled();
}
