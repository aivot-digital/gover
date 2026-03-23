package de.aivot.GoverBackend.elements.models.elements.layout;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.LayoutElement;
import de.aivot.GoverBackend.elements.models.elements.steps.IntroductionStepElement;
import de.aivot.GoverBackend.elements.models.elements.steps.StepElement;
import de.aivot.GoverBackend.elements.models.elements.steps.SubmitStepElement;
import de.aivot.GoverBackend.elements.models.elements.steps.SummaryStepElement;
import de.aivot.GoverBackend.enums.ElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class FormLayoutElement extends BaseElement implements LayoutElement<StepElement> {
    private String tabTitle;
    private List<StepElement> children = new LinkedList<>();

    private String expiring;

    private String privacyText;

    private String offlineSubmissionText;
    private Boolean offlineSignatureNeeded;

    private IntroductionStepElement introductionStep;
    private SummaryStepElement summaryStep;
    private SubmitStepElement submitStep;

    public FormLayoutElement() {
        super(ElementType.FormLayout);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        FormLayoutElement that = (FormLayoutElement) o;

        if (!Objects.equals(tabTitle, that.tabTitle)) return false;
        if (!Objects.equals(children, that.children)) return false;
        if (!Objects.equals(expiring, that.expiring)) return false;
        if (!Objects.equals(privacyText, that.privacyText)) return false;
        if (!Objects.equals(offlineSubmissionText, that.offlineSubmissionText)) return false;
        if (!Objects.equals(offlineSignatureNeeded, that.offlineSignatureNeeded)) return false;
        if (!Objects.equals(introductionStep, that.introductionStep))
            return false;
        if (!Objects.equals(summaryStep, that.summaryStep)) return false;
        return Objects.equals(submitStep, that.submitStep);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (tabTitle != null ? tabTitle.hashCode() : 0);
        result = 31 * result + (children != null ? children.hashCode() : 0);
        result = 31 * result + (expiring != null ? expiring.hashCode() : 0);
        result = 31 * result + (privacyText != null ? privacyText.hashCode() : 0);
        result = 31 * result + (offlineSubmissionText != null ? offlineSubmissionText.hashCode() : 0);
        result = 31 * result + (offlineSignatureNeeded != null ? offlineSignatureNeeded.hashCode() : 0);
        result = 31 * result + (introductionStep != null ? introductionStep.hashCode() : 0);
        result = 31 * result + (summaryStep != null ? summaryStep.hashCode() : 0);
        result = 31 * result + (submitStep != null ? submitStep.hashCode() : 0);
        return result;
    }

    @JsonIgnore
    public String getCleanedPrivacyText() {
        if (privacyText == null) {
            return null;
        }
        return privacyText.replaceAll("\\{[^}]+}", "");
    }

    // region Getters & Setters

    public String getTabTitle() {
        return tabTitle;
    }

    public FormLayoutElement setTabTitle(String tabTitle) {
        this.tabTitle = tabTitle;
        return this;
    }

    @Nonnull
    @Override
    public List<StepElement> getChildren() {
        if (children == null) {
            children = new LinkedList<>();
        }
        return children;
    }

    @Nonnull
    @Override
    public FormLayoutElement setChildren(@Nullable List<StepElement> children) {
        if (children == null) {
            children = new LinkedList<>();
        }
        this.children = children;
        return this;
    }

    public String getExpiring() {
        return expiring;
    }

    public FormLayoutElement setExpiring(String expiring) {
        this.expiring = expiring;
        return this;
    }

    public String getPrivacyText() {
        return privacyText;
    }

    public FormLayoutElement setPrivacyText(String privacyText) {
        this.privacyText = privacyText;
        return this;
    }

    public String getOfflineSubmissionText() {
        return offlineSubmissionText;
    }

    public FormLayoutElement setOfflineSubmissionText(String offlineSubmissionText) {
        this.offlineSubmissionText = offlineSubmissionText;
        return this;
    }

    public Boolean getOfflineSignatureNeeded() {
        return offlineSignatureNeeded;
    }

    public FormLayoutElement setOfflineSignatureNeeded(Boolean offlineSignatureNeeded) {
        this.offlineSignatureNeeded = offlineSignatureNeeded;
        return this;
    }

    public IntroductionStepElement getIntroductionStep() {
        return introductionStep;
    }

    public FormLayoutElement setIntroductionStep(IntroductionStepElement introductionStep) {
        this.introductionStep = introductionStep;
        return this;
    }

    public SummaryStepElement getSummaryStep() {
        return summaryStep;
    }

    public FormLayoutElement setSummaryStep(SummaryStepElement summaryStep) {
        this.summaryStep = summaryStep;
        return this;
    }

    public SubmitStepElement getSubmitStep() {
        return submitStep;
    }

    public FormLayoutElement setSubmitStep(SubmitStepElement submitStep) {
        this.submitStep = submitStep;
        return this;
    }

    // endregion
}
