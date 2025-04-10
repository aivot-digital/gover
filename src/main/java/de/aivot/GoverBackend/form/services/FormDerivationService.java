package de.aivot.GoverBackend.form.services;


import de.aivot.GoverBackend.elements.models.BaseElement;
import de.aivot.GoverBackend.elements.services.BaseElementDerivationService;
import de.aivot.GoverBackend.form.entities.Form;
import de.aivot.GoverBackend.form.models.FormDerivationContext;
import de.aivot.GoverBackend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.GoverBackend.nocode.services.NoCodeEvaluationService;
import de.aivot.GoverBackend.services.ScriptService;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

public class FormDerivationService extends BaseElementDerivationService<FormDerivationContext> {
    public static final String FORM_STEP_LIMIT_ALL_IDENTIFIER = "ALL";
    public static final String FORM_STEP_LIMIT_NONE_IDENTIFIER = "NONE";

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

    private final JavascriptEngineFactoryService javascriptEngineFactoryService;
    private final NoCodeEvaluationService noCodeEvaluationService;

    public FormDerivationService(
            @Nonnull Form form,
            @Nonnull List<String> stepsToValidate,
            @Nonnull List<String> stepsToCalculateVisibilities,
            @Nonnull List<String> stepsToCalculateValues,
            @Nonnull List<String> stepsToCalculateOverrides,
            @Nonnull JavascriptEngineFactoryService javascriptEngineFactoryService,
            @Nonnull NoCodeEvaluationService noCodeEvaluationService
    ) {
        this.form = form;

        this.stepsToValidate = stepsToValidate;
        this.stepsToCalculateVisibilities = stepsToCalculateVisibilities;
        this.stepsToCalculateValues = stepsToCalculateValues;
        this.stepsToCalculateOverrides = stepsToCalculateOverrides;

        this.javascriptEngineFactoryService = javascriptEngineFactoryService;
        this.noCodeEvaluationService = noCodeEvaluationService;
    }

    @Override
    protected void deriveVisibilities(FormDerivationContext context, BaseElement currentElement) {
        if (stepsToCalculateVisibilities.isEmpty() || stepsToCalculateVisibilities.contains(FORM_STEP_LIMIT_NONE_IDENTIFIER)) {
            return;
        }

        new FormVisibilityDerivationService()
                .derive(context, null, currentElement);
    }

    @Override
    protected void deriveErrors(FormDerivationContext context, BaseElement currentElement) {
        if (stepsToValidate.isEmpty() || stepsToValidate.contains(FORM_STEP_LIMIT_NONE_IDENTIFIER)) {
            return;
        }

        new FormErrorDerivationService()
                .derive(context, null, currentElement);
    }

    @Override
    protected void deriveOverrides(FormDerivationContext context, BaseElement currentElement) {
        if (stepsToCalculateOverrides.isEmpty() || stepsToCalculateOverrides.contains(FORM_STEP_LIMIT_NONE_IDENTIFIER)) {
            return;
        }

        new FormOverrideDerivationService()
                .derive(context, null, currentElement);
    }

    @Override
    protected void deriveValues(FormDerivationContext context, BaseElement currentElement) {
        if (stepsToCalculateValues.isEmpty() || stepsToCalculateValues.contains(FORM_STEP_LIMIT_NONE_IDENTIFIER)) {
            return;
        }

        new FormValueDerivationService()
                .derive(context, null, currentElement);
    }

    @Override
    protected FormDerivationContext prepareContext(@NotNull Map<String, Object> inputValues) {
        return new FormDerivationContext(
                javascriptEngineFactoryService.getEngine(),
                ScriptService.getEngine(),
                noCodeEvaluationService,
                form.getRoot(),
                inputValues,
                form,
                stepsToValidate,
                stepsToCalculateVisibilities,
                stepsToCalculateValues,
                stepsToCalculateOverrides
        );
    }
}
