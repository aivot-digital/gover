package de.aivot.GoverBackend.models.elements;

import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.elements.form.BaseFormElement;
import de.aivot.GoverBackend.models.elements.steps.IntroductionStepElement;
import de.aivot.GoverBackend.models.elements.steps.StepElement;
import de.aivot.GoverBackend.models.elements.steps.SubmitStepElement;
import de.aivot.GoverBackend.models.elements.steps.SummaryStepElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.script.ScriptEngine;
import java.util.*;

public class RootElement extends BaseElement {
    private String headline;
    private String tabTitle;
    private String theme;
    private Collection<StepElement> children;

    private String expiring;
    private String accessLevel;

    private String privacyText;

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
        theme = MapUtils.getString(values, "theme");


        children = MapUtils.getCollection(values, "children", StepElement::new);

        expiring = MapUtils.getString(values, "expiring");
        accessLevel = MapUtils.getString(values, "accessLevel");

        privacyText = MapUtils.getString(values, "privacyText");

        introductionStep = MapUtils.getApply(values, "introductionStep", Map.class, IntroductionStepElement::new);
        summaryStep = MapUtils.getApply(values, "summaryStep", Map.class, SummaryStepElement::new);
        submitStep = MapUtils.getApply(values, "submitStep", Map.class, SubmitStepElement::new);
    }

    @Override
    public void validate(String idPrefix, RootElement root, Map<String, Object> customerInput, ScriptEngine scriptEngine) throws ValidationException {
        introductionStep.validate(idPrefix, this, customerInput, scriptEngine);
        summaryStep.validate(idPrefix, this, customerInput, scriptEngine);
        submitStep.validate(idPrefix, this, customerInput, scriptEngine);

        if (children != null) {
            for (var child : children) {
                child.patch(idPrefix, this, customerInput, scriptEngine);
                if (child.isVisible(idPrefix, this, customerInput, scriptEngine)) {
                    child.validate(idPrefix, root, customerInput, scriptEngine);
                }
            }
        }
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(RootElement root, Map<String, Object> customerInput, String idPrefix, ScriptEngine scriptEngine) {
        List<BasePdfRowDto> rows = new LinkedList<>();

        for (StepElement child : children) {
            child.patch(idPrefix, root, customerInput, scriptEngine);
            if (child.isVisible(idPrefix, root, customerInput, scriptEngine)) {
                rows.addAll(child.toPdfRows(root, customerInput, idPrefix, scriptEngine));
            }
        }

        return rows;
    }

    public Optional<? extends BaseElement> findChild(String id) {
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
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (obj instanceof RootElement rObj) {
            return rObj.getId().equals(getId());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
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

    // endregion
}
