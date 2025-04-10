package de.aivot.GoverBackend.form.models;

import de.aivot.GoverBackend.elements.models.BaseElement;
import de.aivot.GoverBackend.elements.models.BaseElementDerivationContext;
import de.aivot.GoverBackend.form.entities.Form;
import de.aivot.GoverBackend.javascript.services.JavascriptEngine;
import de.aivot.GoverBackend.nocode.services.NoCodeEvaluationService;

import javax.annotation.Nonnull;
import javax.script.ScriptEngine;
import java.util.List;
import java.util.Map;


public class FormDerivationContext extends BaseElementDerivationContext {
    public static final String FORM_JS_CONTEXT_OBJECT_NAME = "form";

    @Nonnull
    private final Form form;

    @Nonnull
    private final List<String> stepsToValidate;
    @Nonnull
    private final List<String> stepsToCalculateVisibilities;
    @Nonnull
    private final List<String> stepsToCalculateValues;
    @Nonnull
    private final List<String> stepsToCalculateOverrides;

    public FormDerivationContext(
            @Nonnull JavascriptEngine javascriptEngine,
            @Nonnull ScriptEngine legacyJavascriptEngine,
            @Nonnull NoCodeEvaluationService noCodeEvaluationService,
            @Nonnull BaseElement rootElement,
            @Nonnull Map<String, Object> inputValues,
            @Nonnull Form form,
            @Nonnull List<String> stepsToValidate,
            @Nonnull List<String> stepsToCalculateVisibilities,
            @Nonnull List<String> stepsToCalculateValues,
            @Nonnull List<String> stepsToCalculateOverrides
    ) {
        super(javascriptEngine, legacyJavascriptEngine, noCodeEvaluationService, rootElement, inputValues);
        this.form = form;

        this.stepsToValidate = stepsToValidate;
        this.stepsToCalculateVisibilities = stepsToCalculateVisibilities;
        this.stepsToCalculateValues = stepsToCalculateValues;
        this.stepsToCalculateOverrides = stepsToCalculateOverrides;
    }

    @Nonnull
    public Form getForm() {
        return form;
    }

    @Nonnull
    public List<String> getStepsToValidate() {
        return stepsToValidate;
    }

    @Nonnull
    public List<String> getStepsToCalculateVisibilities() {
        return stepsToCalculateVisibilities;
    }

    @Nonnull
    public List<String> getStepsToCalculateValues() {
        return stepsToCalculateValues;
    }

    @Nonnull
    public List<String> getStepsToCalculateOverrides() {
        return stepsToCalculateOverrides;
    }

    @Nonnull
    @Override
    public Map<String, Object> getJavascriptContextObject(String resolvedId, BaseElement currentElement) {
        var map = super.getJavascriptContextObject(resolvedId, currentElement);
        map.put(FORM_JS_CONTEXT_OBJECT_NAME, form);
        return map;
    }

    public FormState getFormState() {
        return new FormState(
                super.getElementDerivationData().getVisibilities(),
                super.getCombinedValues(),
                super.getElementDerivationData().getErrors(),
                super.getElementDerivationData().getOverrides()
        );
    }
}
