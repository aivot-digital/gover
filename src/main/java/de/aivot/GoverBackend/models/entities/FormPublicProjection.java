package de.aivot.GoverBackend.models.entities;

import de.aivot.GoverBackend.enums.*;
import de.aivot.GoverBackend.models.elements.RootElement;

import java.time.LocalDateTime;

public interface FormPublicProjection {
    Integer getId();

    String getSlug();

    String getVersion();

    String getTitle();

    ApplicationStatus getStatus();

    RootElement getRoot();

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

    Boolean getBayernIdEnabled();

    Boolean getMukEnabled();

    BayernIdAccessLevel getBayernIdLevel();

    BundIdAccessLevel getBundIdLevel();

    Boolean getShIdEnabled();

    SchleswigHolsteinIdAccessLevel getShIdLevel();

    MukAccessLevel getMukLevel();
}
