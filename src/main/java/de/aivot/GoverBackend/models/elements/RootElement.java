package de.aivot.GoverBackend.models.elements;

import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.elements.steps.IntroductionStepElement;
import de.aivot.GoverBackend.models.elements.steps.StepElement;
import de.aivot.GoverBackend.models.elements.steps.SubmitStepElement;
import de.aivot.GoverBackend.models.elements.steps.SummaryStepElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.script.ScriptEngine;
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

    public RootElement(Map<String, Object> values) {
        super(values);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        title = MapUtils.getString(values, "title");
        headline = MapUtils.getString(values, "headline");
        tabTitle = MapUtils.getString(values, "tabTitle");
        theme = MapUtils.getString(values, "theme");


        children = MapUtils.getCollection(values, "children", StepElement::new);

        expiring = MapUtils.getString(values, "expiring");
        accessLevel = MapUtils.getString(values, "accessLevel");

        legalSupport = MapUtils.getInteger(values, "legalSupport");
        technicalSupport = MapUtils.getInteger(values, "technicalSupport");

        privacy = MapUtils.getInteger(values, "privacy");
        imprint = MapUtils.getInteger(values, "imprint");
        accessibility = MapUtils.getInteger(values, "accessibility");

        privacyText = MapUtils.getString(values, "privacyText");

        destination = MapUtils.getInteger(values, "destination");

        introductionStep = MapUtils.getApply(values, "introductionStep", Map.class, IntroductionStepElement::new);
        summaryStep = MapUtils.getApply(values, "summaryStep", Map.class, SummaryStepElement::new);
        submitStep = MapUtils.getApply(values, "submitStep", Map.class, SubmitStepElement::new);
    }

    @Override
    public void validate(Map<String, Object> customerInput, String idPrefix, ScriptEngine scriptEngine) throws ValidationException {
        introductionStep.validate(customerInput, idPrefix, scriptEngine);
        summaryStep.validate(customerInput, idPrefix, scriptEngine);
        submitStep.validate(customerInput, idPrefix, scriptEngine);

        if (children != null) {
            for (var child : children) {
                child.validate(customerInput, idPrefix, scriptEngine);
            }
        }
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(Map<String, Object> customerInput, String idPrefix, ScriptEngine scriptEngine) {
        List<BasePdfRowDto> rows = new LinkedList<>();

        for (StepElement step : children) {
            rows.addAll(step.toPdfRows(customerInput, idPrefix, scriptEngine));
        }

        return rows;
    }

    // region Getters & Setters

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

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

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
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

    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    public Integer getLegalSupport() {
        return legalSupport;
    }

    public void setLegalSupport(Integer legalSupport) {
        this.legalSupport = legalSupport;
    }

    public Integer getTechnicalSupport() {
        return technicalSupport;
    }

    public void setTechnicalSupport(Integer technicalSupport) {
        this.technicalSupport = technicalSupport;
    }

    public Integer getImprint() {
        return imprint;
    }

    public void setImprint(Integer imprint) {
        this.imprint = imprint;
    }

    public Integer getPrivacy() {
        return privacy;
    }

    public void setPrivacy(Integer privacy) {
        this.privacy = privacy;
    }

    public Integer getAccessibility() {
        return accessibility;
    }

    public void setAccessibility(Integer accessibility) {
        this.accessibility = accessibility;
    }

    public String getPrivacyText() {
        return privacyText;
    }

    public void setPrivacyText(String privacyText) {
        this.privacyText = privacyText;
    }

    public Integer getDestination() {
        return destination;
    }

    public void setDestination(Integer destination) {
        this.destination = destination;
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

    // endregion
}
