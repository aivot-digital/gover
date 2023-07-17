package de.aivot.GoverBackend.models.dtos;

import de.aivot.GoverBackend.models.entities.Application;


public class ApplicationDetailsMinimalDto extends ApplicationListDto {
    private Integer destination;
    private Integer legalSupportDepartment;
    private Integer technicalSupportDepartment;
    private Integer imprintDepartment;
    private Integer privacyDepartment;
    private Integer accessibilityDepartment;
    private Integer customerAccessHours;
    private Integer submissionDeletionWeeks;

    public ApplicationDetailsMinimalDto() {
    }

    public ApplicationDetailsMinimalDto(Application app) {
        super(app);

        if (app.getDestination() != null) {
            destination = app.getDestination().getId();
        }

        if (app.getLegalSupportDepartment() != null) {
            legalSupportDepartment = app.getLegalSupportDepartment().getId();
        }
        if (app.getTechnicalSupportDepartment() != null) {
            technicalSupportDepartment = app.getTechnicalSupportDepartment().getId();
        }

        if (app.getImprintDepartment() != null) {
            imprintDepartment = app.getImprintDepartment().getId();
        }
        if (app.getPrivacyDepartment() != null) {
            privacyDepartment = app.getPrivacyDepartment().getId();
        }
        if (app.getAccessibilityDepartment() != null) {
            accessibilityDepartment = app.getAccessibilityDepartment().getId();
        }

        customerAccessHours = app.getCustomerAccessHours();
        submissionDeletionWeeks = app.getSubmissionDeletionWeeks();
    }

    // region Getters & Setters

    public Integer getDestination() {
        return destination;
    }

    public void setDestination(Integer destination) {
        this.destination = destination;
    }

    public Integer getLegalSupportDepartment() {
        return legalSupportDepartment;
    }

    public void setLegalSupportDepartment(Integer legalSupportDepartment) {
        this.legalSupportDepartment = legalSupportDepartment;
    }

    public Integer getTechnicalSupportDepartment() {
        return technicalSupportDepartment;
    }

    public void setTechnicalSupportDepartment(Integer technicalSupportDepartment) {
        this.technicalSupportDepartment = technicalSupportDepartment;
    }

    public Integer getImprintDepartment() {
        return imprintDepartment;
    }

    public void setImprintDepartment(Integer imprintDepartment) {
        this.imprintDepartment = imprintDepartment;
    }

    public Integer getPrivacyDepartment() {
        return privacyDepartment;
    }

    public void setPrivacyDepartment(Integer privacyDepartment) {
        this.privacyDepartment = privacyDepartment;
    }

    public Integer getAccessibilityDepartment() {
        return accessibilityDepartment;
    }

    public void setAccessibilityDepartment(Integer accessibilityDepartment) {
        this.accessibilityDepartment = accessibilityDepartment;
    }

    public Integer getCustomerAccessHours() {
        return customerAccessHours;
    }

    public void setCustomerAccessHours(Integer customerAccessHours) {
        this.customerAccessHours = customerAccessHours;
    }

    public Integer getSubmissionDeletionWeeks() {
        return submissionDeletionWeeks;
    }

    public void setSubmissionDeletionWeeks(Integer submissionDeletionWeeks) {
        this.submissionDeletionWeeks = submissionDeletionWeeks;
    }

    // endregion
}
