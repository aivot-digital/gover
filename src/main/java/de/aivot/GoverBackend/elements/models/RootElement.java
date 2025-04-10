package de.aivot.GoverBackend.elements.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.form.models.FormState;
import de.aivot.GoverBackend.elements.models.steps.IntroductionStepElement;
import de.aivot.GoverBackend.elements.models.steps.StepElement;
import de.aivot.GoverBackend.elements.models.steps.SubmitStepElement;
import de.aivot.GoverBackend.elements.models.steps.SummaryStepElement;
import de.aivot.GoverBackend.models.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.annotation.Nonnull;
import java.util.*;

public class RootElement extends BaseElement {
    private String headline;
    private String tabTitle;
    private Collection<StepElement> children;

    private String expiring;

    private String privacyText;

    private String offlineSubmissionText;
    private Boolean offlineSignatureNeeded;

    private IntroductionStepElement introductionStep;
    private SummaryStepElement summaryStep;
    private SubmitStepElement submitStep;

    public RootElement(Map<String, Object> values) {
        super(values);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        headline = MapUtils.getString(values, "headline");
        tabTitle = MapUtils.getString(values, "tabTitle");

        children = MapUtils.getCollection(values, "children", StepElement::new);

        expiring = MapUtils.getString(values, "expiring");

        privacyText = MapUtils.getString(values, "privacyText");
        offlineSubmissionText = MapUtils.getString(values, "offlineSubmissionText");
        offlineSignatureNeeded = MapUtils.getBoolean(values, "offlineSignatureNeeded");

        introductionStep = MapUtils.getApply(values, "introductionStep", Map.class, IntroductionStepElement::new);
        summaryStep = MapUtils.getApply(values, "summaryStep", Map.class, SummaryStepElement::new);
        submitStep = MapUtils.getApply(values, "submitStep", Map.class, SubmitStepElement::new);
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(RootElement root, String idPrefix, FormState formState) {
        List<BasePdfRowDto> rows = new LinkedList<>();

        for (StepElement child : children) {
            var isVisible = formState.visibilities().getOrDefault(child.getResolvedId(idPrefix), true);
            if (!isVisible) {
                continue;
            }

            var resolvedChild = formState.overrides().getOrDefault(child.getResolvedId(idPrefix), child);
            rows.addAll(resolvedChild.toPdfRows(root, idPrefix, formState));
        }

        return rows;
    }

    @Override
    public Optional<? extends BaseElement> findChild(@Nonnull String id) {
        Optional<StepElement> matchingStep = children
                .stream()
                .filter(s -> s.matches(id))
                .findFirst();

        if (matchingStep.isPresent()) {
            return matchingStep;
        }

        return children
                .stream()
                .map(s -> s.findChild(id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RootElement that = (RootElement) o;

        if (!Objects.equals(headline, that.headline)) return false;
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
        result = 31 * result + (headline != null ? headline.hashCode() : 0);
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
        return privacyText.replaceAll("\\{[^}]+}", "");
    }

    // region Getters & Setters

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getTabTitle() {
        return tabTitle;
    }

    public void setTabTitle(String tabTitle) {
        this.tabTitle = tabTitle;
    }

    public Collection<StepElement> getChildren() {
        return children;
    }

    public void setChildren(Collection<StepElement> children) {
        this.children = children;
    }

    public String getExpiring() {
        return expiring;
    }

    public void setExpiring(String expiring) {
        this.expiring = expiring;
    }

    public String getPrivacyText() {
        return privacyText;
    }

    public void setPrivacyText(String privacyText) {
        this.privacyText = privacyText;
    }

    public IntroductionStepElement getIntroductionStep() {
        return introductionStep;
    }

    public void setIntroductionStep(IntroductionStepElement introductionStep) {
        this.introductionStep = introductionStep;
    }

    public SummaryStepElement getSummaryStep() {
        return summaryStep;
    }

    public void setSummaryStep(SummaryStepElement summaryStep) {
        this.summaryStep = summaryStep;
    }

    public SubmitStepElement getSubmitStep() {
        return submitStep;
    }

    public void setSubmitStep(SubmitStepElement submitStep) {
        this.submitStep = submitStep;
    }

    public String getOfflineSubmissionText() {
        return offlineSubmissionText;
    }

    public void setOfflineSubmissionText(String offlineSubmissionText) {
        this.offlineSubmissionText = offlineSubmissionText;
    }

    public Boolean getOfflineSignatureNeeded() {
        return offlineSignatureNeeded;
    }

    public void setOfflineSignatureNeeded(Boolean offlineSignatureNeeded) {
        this.offlineSignatureNeeded = offlineSignatureNeeded;
    }

    // endregion
}
