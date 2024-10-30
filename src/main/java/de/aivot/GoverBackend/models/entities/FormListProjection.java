package de.aivot.GoverBackend.models.entities;

import de.aivot.GoverBackend.enums.*;

import java.time.LocalDateTime;

public interface FormListProjection {
    Integer getId();
    String getSlug();
    String getVersion();
    String getTitle();
    ApplicationStatus getStatus();
    Integer getDestinationId();
    Integer getLegalSupportDepartmentId();
    Integer getTechnicalSupportDepartmentId();
    Integer getImprintDepartmentId();
    Integer getPrivacyDepartmentId();
    Integer getAccessibilityDepartmentId();
    Integer getDevelopingDepartmentId();
    Integer getManagingDepartmentId();
    Integer getResponsibleDepartmentId();
    Integer getThemeId();
    LocalDateTime getCreated();
    LocalDateTime getUpdated();
    Integer getCustomerAccessHours();
    Integer getSubmissionDeletionWeeks();
    Boolean getBundIdEnabled();
    BundIdAccessLevel getBundIdLevel();
    Boolean getBayernIdEnabled();
    BayernIdAccessLevel getBayernIdLevel();
    Boolean getMukEnabled();
    Boolean getShIdEnabled();
    SchleswigHolsteinIdAccessLevel getShIdLevel();
    MukAccessLevel getMukLevel();
}
