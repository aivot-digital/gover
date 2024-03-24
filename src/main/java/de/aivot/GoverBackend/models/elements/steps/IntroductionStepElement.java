package de.aivot.GoverBackend.models.elements.steps;

import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        IntroductionStepElement that = (IntroductionStepElement) o;

        if (!Objects.equals(initiativeName, that.initiativeName))
            return false;
        if (!Objects.equals(initiativeLogoLink, that.initiativeLogoLink))
            return false;
        if (!Objects.equals(initiativeLink, that.initiativeLink))
            return false;
        if (!Objects.equals(teaserText, that.teaserText)) return false;
        if (!Objects.equals(organization, that.organization)) return false;
        if (!Objects.equals(eligiblePersons, that.eligiblePersons))
            return false;
        if (!Objects.equals(supportingDocuments, that.supportingDocuments))
            return false;
        if (!Objects.equals(documentsToAttach, that.documentsToAttach))
            return false;
        return Objects.equals(expectedCosts, that.expectedCosts);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (initiativeName != null ? initiativeName.hashCode() : 0);
        result = 31 * result + (initiativeLogoLink != null ? initiativeLogoLink.hashCode() : 0);
        result = 31 * result + (initiativeLink != null ? initiativeLink.hashCode() : 0);
        result = 31 * result + (teaserText != null ? teaserText.hashCode() : 0);
        result = 31 * result + (organization != null ? organization.hashCode() : 0);
        result = 31 * result + (eligiblePersons != null ? eligiblePersons.hashCode() : 0);
        result = 31 * result + (supportingDocuments != null ? supportingDocuments.hashCode() : 0);
        result = 31 * result + (documentsToAttach != null ? documentsToAttach.hashCode() : 0);
        result = 31 * result + (expectedCosts != null ? expectedCosts.hashCode() : 0);
        return result;
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
