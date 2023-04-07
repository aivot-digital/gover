package de.aivot.GoverBackend.models.elements;

import com.sun.istack.Nullable;
import de.aivot.GoverBackend.models.elements.steps.IntroductionStepElement;
import de.aivot.GoverBackend.models.elements.steps.StepElement;
import de.aivot.GoverBackend.models.elements.steps.SubmitStepElement;
import de.aivot.GoverBackend.models.elements.steps.SummaryStepElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RootElement extends BaseElement {
    private String title;
    private String headline;
    private String tabTitle;
    private String theme;
    private Collection<StepElement> children;

    private String expiring;
    private String accessLevel;

    private Integer legalSupport;
    private Integer technicalSupport;

    private Integer imprint;
    private Integer privacy;
    private Integer accessibility;

    private String privacyText;

    private Integer destination;

    private IntroductionStepElement introductionStep;
    private SummaryStepElement summaryStep;
    private SubmitStepElement submitStep;

    public RootElement(Map<String, Object> data) {
        super(data);

        title = (String) data.get("title");
        headline = (String) data.get("headline");
        tabTitle = (String) data.get("tabTitle");
        theme = (String) data.get("theme");

        if (data.get("children") != null) {
            children = new LinkedList<>();
            for (Map<String, Object> childData : (Collection<Map<String, Object>>) data.get("children")) {
                if (childData != null) {
                    children.add(new StepElement(childData));
                }
            }
        }

        expiring = (String) data.get("expiring");
        accessLevel = (String) data.get("accessLevel");

        legalSupport = (Integer) data.get("legalSupport");
        technicalSupport = (Integer) data.get("technicalSupport");

        privacy = (Integer) data.get("privacy");
        imprint = (Integer) data.get("imprint");
        accessibility = (Integer) data.get("accessibility");

        privacyText = (String) data.get("privacyText");

        destination = (Integer) data.get("destination");

        Map<String, Object> introductionStepData = (Map<String, Object>) data.get("introductionStep");
        if (introductionStepData != null) {
            introductionStep = new IntroductionStepElement(introductionStepData);
        }

        Map<String, Object> summaryStepData = (Map<String, Object>) data.get("summaryStep");
        if (summaryStepData != null) {
            summaryStep = new SummaryStepElement(summaryStepData);
        }

        Map<String, Object> submitStepData = (Map<String, Object>) data.get("submitStep");
        if (submitStepData != null) {
            submitStep = new SubmitStepElement(submitStepData);
        }
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Nullable
    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    @Nullable
    public String getTabTitle() {
        return tabTitle;
    }

    public void setTabTitle(String tabTitle) {
        this.tabTitle = tabTitle;
    }

    @Nullable
    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    @Nullable
    public Collection<StepElement> getChildren() {
        return children;
    }

    public void setChildren(Collection<StepElement> children) {
        this.children = children;
    }

    @Nullable
    public String getExpiring() {
        return expiring;
    }

    public void setExpiring(String expiring) {
        this.expiring = expiring;
    }

    @Nullable
    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    @Nullable
    public Integer getLegalSupport() {
        return legalSupport;
    }

    public void setLegalSupport(Integer legalSupport) {
        this.legalSupport = legalSupport;
    }

    @Nullable
    public Integer getTechnicalSupport() {
        return technicalSupport;
    }

    public void setTechnicalSupport(Integer technicalSupport) {
        this.technicalSupport = technicalSupport;
    }

    @Nullable
    public Integer getImprint() {
        return imprint;
    }

    public void setImprint(Integer imprint) {
        this.imprint = imprint;
    }

    @Nullable
    public Integer getPrivacy() {
        return privacy;
    }

    public void setPrivacy(Integer privacy) {
        this.privacy = privacy;
    }

    @Nullable
    public Integer getAccessibility() {
        return accessibility;
    }

    public void setAccessibility(Integer accessibility) {
        this.accessibility = accessibility;
    }

    @Nullable
    public String getPrivacyText() {
        return privacyText;
    }

    public void setPrivacyText(String privacyText) {
        this.privacyText = privacyText;
    }

    @Nullable
    public Integer getDestination() {
        return destination;
    }

    public void setDestination(Integer destination) {
        this.destination = destination;
    }

    @Nullable
    public IntroductionStepElement getIntroductionStep() {
        return introductionStep;
    }

    public void setIntroductionStep(IntroductionStepElement introductionStep) {
        this.introductionStep = introductionStep;
    }

    @Nullable
    public SummaryStepElement getSummaryStep() {
        return summaryStep;
    }

    public void setSummaryStep(SummaryStepElement summaryStep) {
        this.summaryStep = summaryStep;
    }

    @Nullable
    public SubmitStepElement getSubmitStep() {
        return submitStep;
    }

    public void setSubmitStep(SubmitStepElement submitStep) {
        this.submitStep = submitStep;
    }

    @Override
    public boolean isValid(Map<String, Object> customerInput, @Nullable String idPrefix) {
        return (
                introductionStep.isValid(customerInput, idPrefix) &&
                        summaryStep.isValid(customerInput, idPrefix) &&
                        submitStep.isValid(customerInput, idPrefix) &&
                        children.stream().allMatch(c -> c.isValid(customerInput, idPrefix))
        );
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(Map<String, Object> customerInput, @Nullable String idPrefix) {
        List<BasePdfRowDto> rows = new LinkedList<>();

        for (StepElement step : children) {
            rows.addAll(step.toPdfRows(customerInput, idPrefix));
        }

        return rows;
    }
}
