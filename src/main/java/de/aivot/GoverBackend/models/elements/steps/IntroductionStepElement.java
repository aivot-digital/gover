package de.aivot.GoverBackend.models.elements.steps;

import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Collection;
import java.util.Map;

public class IntroductionStepElement extends BaseElement {
    private String initiativeName;
    private String initiativeLogoLink;
    private String initiativeLink;
    private String teaserText;
    private String organization;
    private Collection<String> eligiblePersons;
    private Collection<String> supportingDocuments;
    private Collection<String> documentsToAttach;
    private String expectedCosts;

    public IntroductionStepElement(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        initiativeName = MapUtils.getString(values, "initiativeName");
        initiativeLogoLink = MapUtils.getString(values, "initiativeLogoLink");
        initiativeLink = MapUtils.getString(values, "initiativeLink");
        teaserText = MapUtils.getString(values, "teaserText");
        organization = MapUtils.getString(values, "organization");
        eligiblePersons = MapUtils.get(values, "eligiblePersons", Collection.class);
        supportingDocuments = MapUtils.get(values, "supportingDocuments", Collection.class);
        documentsToAttach = MapUtils.get(values, "documentsToAttach", Collection.class);
        expectedCosts = MapUtils.getString(values, "expectedCosts");
    }

    // region Getters & Setters

    public String getInitiativeName() {
        return initiativeName;
    }

    public void setInitiativeName(String initiativeName) {
        this.initiativeName = initiativeName;
    }

    public String getInitiativeLogoLink() {
        return initiativeLogoLink;
    }

    public void setInitiativeLogoLink(String initiativeLogoLink) {
        this.initiativeLogoLink = initiativeLogoLink;
    }

    public String getInitiativeLink() {
        return initiativeLink;
    }

    public void setInitiativeLink(String initiativeLink) {
        this.initiativeLink = initiativeLink;
    }

    public String getTeaserText() {
        return teaserText;
    }

    public void setTeaserText(String teaserText) {
        this.teaserText = teaserText;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public Collection<String> getEligiblePersons() {
        return eligiblePersons;
    }

    public void setEligiblePersons(Collection<String> eligiblePersons) {
        this.eligiblePersons = eligiblePersons;
    }

    public Collection<String> getSupportingDocuments() {
        return supportingDocuments;
    }

    public void setSupportingDocuments(Collection<String> supportingDocuments) {
        this.supportingDocuments = supportingDocuments;
    }

    public Collection<String> getDocumentsToAttach() {
        return documentsToAttach;
    }

    public void setDocumentsToAttach(Collection<String> documentsToAttach) {
        this.documentsToAttach = documentsToAttach;
    }

    public String getExpectedCosts() {
        return expectedCosts;
    }

    public void setExpectedCosts(String expectedCosts) {
        this.expectedCosts = expectedCosts;
    }

    // endregion
}
