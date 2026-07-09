package de.aivot.gover.backend.elements.models.elements.layout;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.gover.backend.elements.models.elements.BaseElement;
import de.aivot.gover.backend.elements.models.elements.LayoutElement;
import de.aivot.gover.backend.elements.models.elements.steps.BaseStepElement;
import de.aivot.gover.backend.elements.models.elements.steps.IntroductionStepElement;
import de.aivot.gover.backend.elements.models.elements.steps.SubmitStepElement;
import de.aivot.gover.backend.elements.models.elements.steps.SummaryStepElement;
import de.aivot.gover.backend.enums.ElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class FormLayoutElement extends BaseElement implements LayoutElement<BaseStepElement> {
    private String tabTitle;
    private List<BaseStepElement> children = new LinkedList<>();

    private String offlineSubmissionText;
    private Boolean offlineSignatureNeeded;

    private String publicTitle;
    private Boolean showOnFormIndexPage = true;

    private Integer managingDepartmentId;
    private Integer responsibleDepartmentId;
    private Integer legalSupportDepartmentId;
    private Integer technicalSupportDepartmentId;
    private Integer imprintDepartmentId;
    private Integer privacyDepartmentId;
    private Integer accessibilityDepartmentId;
    private String formSpecificPrivacyStatement;
    private String formSpecificAccessibilityStatement;

    private Integer themeId;

    private UUID pdfTemplateKey;

    public FormLayoutElement() {
        super(ElementType.FormLayout);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        FormLayoutElement that = (FormLayoutElement) o;
        return Objects.equals(tabTitle, that.tabTitle) && Objects.equals(children, that.children) &&
                Objects.equals(offlineSubmissionText, that.offlineSubmissionText) && Objects.equals(offlineSignatureNeeded, that.offlineSignatureNeeded) &&
                Objects.equals(publicTitle, that.publicTitle) && Objects.equals(showOnFormIndexPage, that.showOnFormIndexPage) &&
                Objects.equals(managingDepartmentId, that.managingDepartmentId) &&
                Objects.equals(responsibleDepartmentId, that.responsibleDepartmentId) && Objects.equals(legalSupportDepartmentId, that.legalSupportDepartmentId) &&
                Objects.equals(technicalSupportDepartmentId, that.technicalSupportDepartmentId) && Objects.equals(imprintDepartmentId, that.imprintDepartmentId) &&
                Objects.equals(privacyDepartmentId, that.privacyDepartmentId) && Objects.equals(accessibilityDepartmentId, that.accessibilityDepartmentId) &&
                Objects.equals(formSpecificPrivacyStatement, that.formSpecificPrivacyStatement) && Objects.equals(formSpecificAccessibilityStatement, that.formSpecificAccessibilityStatement) &&
                Objects.equals(themeId, that.themeId) && Objects.equals(pdfTemplateKey, that.pdfTemplateKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tabTitle, children, offlineSubmissionText, offlineSignatureNeeded, publicTitle, showOnFormIndexPage, managingDepartmentId, responsibleDepartmentId, legalSupportDepartmentId, technicalSupportDepartmentId, imprintDepartmentId, privacyDepartmentId, accessibilityDepartmentId, formSpecificPrivacyStatement, formSpecificAccessibilityStatement, themeId, pdfTemplateKey);
    }

    @JsonIgnore
    public String getCleanedPrivacyText() {
        if (getPrivacyText() == null) {
            return null;
        }
        return getPrivacyText().replaceAll("\\{[^}]+}", "");
    }

    @Nullable
    public Integer getRelevantDepartmentId() {
        if (this.getManagingDepartmentId() != null) {
            return this.getManagingDepartmentId();
        }

        if (this.getResponsibleDepartmentId() != null) {
            return this.getResponsibleDepartmentId();
        }

        return null;
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

    @JsonIgnore
    public String getPrivacyText() {
        return findChild(c -> c.getType() == ElementType.IntroductionStep)
                .filter(IntroductionStepElement.class::isInstance)
                .map(c -> ((IntroductionStepElement) c).getPrivacyText())
                .orElse(null);
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

    public String getPublicTitle() {
        return publicTitle;
    }

    public FormLayoutElement setPublicTitle(String publicTitle) {
        this.publicTitle = publicTitle;
        return this;
    }

    public Boolean getShowOnFormIndexPage() {
        return showOnFormIndexPage;
    }

    public FormLayoutElement setShowOnFormIndexPage(Boolean showOnFormIndexPage) {
        this.showOnFormIndexPage = showOnFormIndexPage;
        return this;
    }

    public Integer getManagingDepartmentId() {
        return managingDepartmentId;
    }

    public FormLayoutElement setManagingDepartmentId(Integer managingDepartmentId) {
        this.managingDepartmentId = managingDepartmentId;
        return this;
    }

    public Integer getResponsibleDepartmentId() {
        return responsibleDepartmentId;
    }

    public FormLayoutElement setResponsibleDepartmentId(Integer responsibleDepartmentId) {
        this.responsibleDepartmentId = responsibleDepartmentId;
        return this;
    }

    public Integer getLegalSupportDepartmentId() {
        return legalSupportDepartmentId;
    }

    public FormLayoutElement setLegalSupportDepartmentId(Integer legalSupportDepartmentId) {
        this.legalSupportDepartmentId = legalSupportDepartmentId;
        return this;
    }

    public Integer getTechnicalSupportDepartmentId() {
        return technicalSupportDepartmentId;
    }

    public FormLayoutElement setTechnicalSupportDepartmentId(Integer technicalSupportDepartmentId) {
        this.technicalSupportDepartmentId = technicalSupportDepartmentId;
        return this;
    }

    public Integer getImprintDepartmentId() {
        return imprintDepartmentId;
    }

    public FormLayoutElement setImprintDepartmentId(Integer imprintDepartmentId) {
        this.imprintDepartmentId = imprintDepartmentId;
        return this;
    }

    public Integer getPrivacyDepartmentId() {
        return privacyDepartmentId;
    }

    public FormLayoutElement setPrivacyDepartmentId(Integer privacyDepartmentId) {
        this.privacyDepartmentId = privacyDepartmentId;
        return this;
    }

    public Integer getAccessibilityDepartmentId() {
        return accessibilityDepartmentId;
    }

    public FormLayoutElement setAccessibilityDepartmentId(Integer accessibilityDepartmentId) {
        this.accessibilityDepartmentId = accessibilityDepartmentId;
        return this;
    }

    public String getFormSpecificPrivacyStatement() {
        return formSpecificPrivacyStatement;
    }

    public FormLayoutElement setFormSpecificPrivacyStatement(String formSpecificPrivacyStatement) {
        this.formSpecificPrivacyStatement = formSpecificPrivacyStatement;
        return this;
    }

    public String getFormSpecificAccessibilityStatement() {
        return formSpecificAccessibilityStatement;
    }

    public FormLayoutElement setFormSpecificAccessibilityStatement(String formSpecificAccessibilityStatement) {
        this.formSpecificAccessibilityStatement = formSpecificAccessibilityStatement;
        return this;
    }

    public Integer getThemeId() {
        return themeId;
    }

    public FormLayoutElement setThemeId(Integer themeId) {
        this.themeId = themeId;
        return this;
    }

    public UUID getPdfTemplateKey() {
        return pdfTemplateKey;
    }

    public FormLayoutElement setPdfTemplateKey(UUID pdfTemplateKey) {
        this.pdfTemplateKey = pdfTemplateKey;
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
