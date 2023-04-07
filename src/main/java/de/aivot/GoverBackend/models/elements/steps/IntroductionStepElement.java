package de.aivot.GoverBackend.models.elements.steps;

import com.sun.istack.Nullable;
import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.RootElement;

import java.util.Collection;
import java.util.Map;

public class IntroductionStepElement extends BaseElement {
    private String initiativeName;
    private String initiativeLogoLink;
    private String initiativeLink;
    private String teaserText;
    private String organization;
    private Integer responsibleDepartment;
    private Integer managingDepartment;
    private Collection<String> eligiblePersons;
    private Collection<String> supportingDocuments;
    private Collection<String> documentsToAttach;
    private String expectedCosts;

    public IntroductionStepElement(Map<String, Object> data) {
        super(data);

        initiativeName = (String) data.get("initiativeName");
        initiativeLogoLink = (String) data.get("initiativeLogoLink");
        initiativeLink = (String) data.get("initiativeLink");
        teaserText = (String) data.get("teaserText");
        organization = (String) data.get("organization");
        responsibleDepartment = (Integer) data.get("responsibleDepartment");
        managingDepartment = (Integer) data.get("managingDepartment");
        eligiblePersons = (Collection<String>) data.get("eligiblePersons");
        supportingDocuments = (Collection<String>) data.get("supportingDocuments");
        documentsToAttach = (Collection<String>) data.get("documentsToAttach");
        expectedCosts = (String) data.get("expectedCosts");
    }

    @Nullable
    public String getInitiativeName() {
        return initiativeName;
    }

    public void setInitiativeName(String initiativeName) {
        this.initiativeName = initiativeName;
    }

    @Nullable
    public String getInitiativeLogoLink() {
        return initiativeLogoLink;
    }

    public void setInitiativeLogoLink(String initiativeLogoLink) {
        this.initiativeLogoLink = initiativeLogoLink;
    }

    @Nullable
    public String getInitiativeLink() {
        return initiativeLink;
    }

    public void setInitiativeLink(String initiativeLink) {
        this.initiativeLink = initiativeLink;
    }

    @Nullable
    public String getTeaserText() {
        return teaserText;
    }

    public void setTeaserText(String teaserText) {
        this.teaserText = teaserText;
    }

    @Nullable
    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    @Nullable
    public Integer getResponsibleDepartment() {
        return responsibleDepartment;
    }

    public void setResponsibleDepartment(Integer responsibleDepartment) {
        this.responsibleDepartment = responsibleDepartment;
    }

    @Nullable
    public Integer getManagingDepartment() {
        return managingDepartment;
    }

    public void setManagingDepartment(Integer managingDepartment) {
        this.managingDepartment = managingDepartment;
    }

    @Nullable
    public Collection<String> getEligiblePersons() {
        return eligiblePersons;
    }

    public void setEligiblePersons(Collection<String> eligiblePersons) {
        this.eligiblePersons = eligiblePersons;
    }

    @Nullable
    public Collection<String> getSupportingDocuments() {
        return supportingDocuments;
    }

    public void setSupportingDocuments(Collection<String> supportingDocuments) {
        this.supportingDocuments = supportingDocuments;
    }

    @Nullable
    public Collection<String> getDocumentsToAttach() {
        return documentsToAttach;
    }

    public void setDocumentsToAttach(Collection<String> documentsToAttach) {
        this.documentsToAttach = documentsToAttach;
    }

    @Nullable
    public String getExpectedCosts() {
        return expectedCosts;
    }

    public void setExpectedCosts(String expectedCosts) {
        this.expectedCosts = expectedCosts;
    }
}
