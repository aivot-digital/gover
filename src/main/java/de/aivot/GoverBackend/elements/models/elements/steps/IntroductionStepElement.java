package de.aivot.GoverBackend.elements.models.elements.steps;

import de.aivot.GoverBackend.elements.models.elements.*;
import de.aivot.GoverBackend.elements.models.elements.form.input.CheckboxInputElement;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class IntroductionStepElement extends BaseStepElement implements InputElement<Boolean>, PrintableElement<Boolean>, LayoutElement<BaseFormElement> {
    private static final String PRIVACY_CONSENT_LABEL = "Datenschutzrechtliche Einwilligung";

    @Nullable
    private String initiativeName;
    @Nullable
    private String initiativeLogoLink;
    @Nullable
    private String initiativeLink;
    @Nullable
    private String teaserText;
    @Nullable
    private String organization;
    @Nullable
    private Collection<String> eligiblePersons;
    @Nullable
    private Collection<String> supportingDocuments;
    @Nullable
    private Collection<String> documentsToAttach;
    @Nullable
    private String expiring;
    @Nullable
    private String expectedCosts;
    @Nullable
    private String privacyText;
    @Nonnull
    private List<BaseFormElement> children = new LinkedList<>();

    public IntroductionStepElement() {
        super(ElementType.IntroductionStep);
    }

    @Override
    public void performValidation(Boolean value) throws ValidationException {
        if (!Boolean.TRUE.equals(value)) {
            throw new ValidationException(
                    this,
                    "Bitte akzeptieren Sie die Hinweise zum Datenschutz.",
                    Map.of("label", PRIVACY_CONSENT_LABEL)
            );
        }
    }

    @Override
    public Boolean getRequired() {
        return true;
    }

    @Override
    public Boolean formatValue(Object value) {
        return CheckboxInputElement._formatValue(value);
    }

    @Nullable
    @Override
    public ElementValueFunctions getValue() {
        return null;
    }

    @Nullable
    @Override
    public ElementValidationFunctions getValidation() {
        return null;
    }

    @Nullable
    @Override
    public Boolean getDisabled() {
        return false;
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable Boolean value) {
        return value == null || !value ? "Nicht akzeptiert" : "Akzeptiert";
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        IntroductionStepElement that = (IntroductionStepElement) o;
        return Objects.equals(initiativeName, that.initiativeName) && Objects.equals(initiativeLogoLink, that.initiativeLogoLink) &&
                Objects.equals(initiativeLink, that.initiativeLink) && Objects.equals(teaserText, that.teaserText) &&
                Objects.equals(organization, that.organization) && Objects.equals(eligiblePersons, that.eligiblePersons) &&
                Objects.equals(supportingDocuments, that.supportingDocuments) && Objects.equals(documentsToAttach, that.documentsToAttach) &&
                Objects.equals(expiring, that.expiring) && Objects.equals(expectedCosts, that.expectedCosts) && Objects.equals(privacyText, that.privacyText) &&
                Objects.equals(children, that.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), initiativeName, initiativeLogoLink, initiativeLink, teaserText, organization, eligiblePersons, supportingDocuments, documentsToAttach, expiring, expectedCosts, privacyText, children);
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public String getInitiativeName() {
        return initiativeName;
    }

    public IntroductionStepElement setInitiativeName(@Nullable String initiativeName) {
        this.initiativeName = initiativeName;
        return this;
    }

    @Nullable
    public String getInitiativeLogoLink() {
        return initiativeLogoLink;
    }

    public IntroductionStepElement setInitiativeLogoLink(@Nullable String initiativeLogoLink) {
        this.initiativeLogoLink = initiativeLogoLink;
        return this;
    }

    @Nullable
    public String getInitiativeLink() {
        return initiativeLink;
    }

    public IntroductionStepElement setInitiativeLink(@Nullable String initiativeLink) {
        this.initiativeLink = initiativeLink;
        return this;
    }

    @Nullable
    public String getTeaserText() {
        return teaserText;
    }

    public IntroductionStepElement setTeaserText(@Nullable String teaserText) {
        this.teaserText = teaserText;
        return this;
    }

    @Nullable
    public String getOrganization() {
        return organization;
    }

    public IntroductionStepElement setOrganization(@Nullable String organization) {
        this.organization = organization;
        return this;
    }

    @Nullable
    public Collection<String> getEligiblePersons() {
        return eligiblePersons;
    }

    public IntroductionStepElement setEligiblePersons(@Nullable Collection<String> eligiblePersons) {
        this.eligiblePersons = eligiblePersons;
        return this;
    }

    @Nullable
    public Collection<String> getSupportingDocuments() {
        return supportingDocuments;
    }

    public IntroductionStepElement setSupportingDocuments(@Nullable Collection<String> supportingDocuments) {
        this.supportingDocuments = supportingDocuments;
        return this;
    }

    @Nullable
    public Collection<String> getDocumentsToAttach() {
        return documentsToAttach;
    }

    public IntroductionStepElement setDocumentsToAttach(@Nullable Collection<String> documentsToAttach) {
        this.documentsToAttach = documentsToAttach;
        return this;
    }

    @Nullable
    public String getExpiring() {
        return expiring;
    }

    public IntroductionStepElement setExpiring(@Nullable String expiring) {
        this.expiring = expiring;
        return this;
    }

    @Nullable
    public String getExpectedCosts() {
        return expectedCosts;
    }

    public IntroductionStepElement setExpectedCosts(@Nullable String expectedCosts) {
        this.expectedCosts = expectedCosts;
        return this;
    }

    @Nullable
    public String getPrivacyText() {
        return privacyText;
    }

    public IntroductionStepElement setPrivacyText(@Nullable String privacyText) {
        this.privacyText = privacyText;
        return this;
    }

    @Override
    @Nonnull
    public List<BaseFormElement> getChildren() {
        return children;
    }

    @Override
    @Nonnull
    public IntroductionStepElement setChildren(List<BaseFormElement> children) {
        this.children = children;
        return this;
    }

    // endregion
}
