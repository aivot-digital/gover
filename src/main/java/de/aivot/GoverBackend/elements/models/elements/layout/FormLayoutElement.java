package de.aivot.GoverBackend.elements.models.elements.layout;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.LayoutElement;
import de.aivot.GoverBackend.elements.models.elements.steps.BaseStepElement;
import de.aivot.GoverBackend.elements.models.elements.steps.IntroductionStepElement;
import de.aivot.GoverBackend.elements.models.elements.steps.SubmitStepElement;
import de.aivot.GoverBackend.elements.models.elements.steps.SummaryStepElement;
import de.aivot.GoverBackend.enums.ElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class FormLayoutElement extends BaseElement implements LayoutElement<BaseStepElement> {
    private String tabTitle;
    private List<BaseStepElement> children = new LinkedList<>();

    private String expiring;

    private String privacyText;

    private String offlineSubmissionText;
    private Boolean offlineSignatureNeeded;

    public FormLayoutElement() {
        super(ElementType.FormLayout);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        FormLayoutElement that = (FormLayoutElement) o;
        return Objects.equals(tabTitle, that.tabTitle) && Objects.equals(children, that.children) && Objects.equals(expiring, that.expiring) && Objects.equals(privacyText, that.privacyText) && Objects.equals(offlineSubmissionText, that.offlineSubmissionText) && Objects.equals(offlineSignatureNeeded, that.offlineSignatureNeeded);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tabTitle, children, expiring, privacyText, offlineSubmissionText, offlineSignatureNeeded);
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
    public List<BaseStepElement> getChildren() {
        if (children == null) {
            children = new LinkedList<>();
        }
        return children;
    }

    @Nonnull
    @Override
    public FormLayoutElement setChildren(@Nullable List<BaseStepElement> children) {
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

    // Compatibility getters keep legacy templates working after step consolidation into children.
    @Nullable
    @JsonIgnore
    public IntroductionStepElement getIntroductionStep() {
        return getStep(IntroductionStepElement.class);
    }

    @Nullable
    @JsonIgnore
    public SummaryStepElement getSummaryStep() {
        return getStep(SummaryStepElement.class);
    }

    @Nullable
    @JsonIgnore
    public SubmitStepElement getSubmitStep() {
        return getStep(SubmitStepElement.class);
    }

    @Nullable
    private <T extends BaseStepElement> T getStep(Class<T> stepClass) {
        return getChildren()
                .stream()
                .filter(stepClass::isInstance)
                .map(stepClass::cast)
                .findFirst()
                .orElse(null);
    }

    // endregion
}
