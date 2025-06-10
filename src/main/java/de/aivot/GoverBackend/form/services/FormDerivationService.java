package de.aivot.GoverBackend.form.services;


import de.aivot.GoverBackend.elements.models.steps.StepElement;
import de.aivot.GoverBackend.elements.services.*;
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
    protected void deriveStepElement(
            @Nonnull FormDerivationContext context,
            @Nonnull StepElement stepElement,
            @Nonnull Boolean deriveVisibilities,
            @Nonnull Boolean deriveOverrides,
            @Nonnull Boolean deriveValues,
            @Nonnull Boolean deriveErrors
    ) {
        var _deriveVisibilities =
                deriveVisibilities &&
                !stepsToCalculateVisibilities.isEmpty() &&
                !stepsToCalculateVisibilities.contains(FORM_STEP_LIMIT_NONE_IDENTIFIER) && (
                        stepsToCalculateVisibilities.contains(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER) ||
                        stepsToCalculateVisibilities.contains(stepElement.getId()) ||
                        !stepElement.getVisibilityReferencedIds().isEmpty()
                );

        var _deriveOverrides =
                deriveVisibilities &&
                !stepsToCalculateOverrides.isEmpty() &&
                !stepsToCalculateOverrides.contains(FORM_STEP_LIMIT_NONE_IDENTIFIER) && (
                        stepsToCalculateOverrides.contains(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER) ||
                        stepsToCalculateOverrides.contains(stepElement.getId()) ||
                        !stepElement.getOverrideReferencedIds().isEmpty()
                );

        var _deriveValues =
                deriveValues &&
                !stepsToCalculateValues.isEmpty() &&
                !stepsToCalculateValues.contains(FORM_STEP_LIMIT_NONE_IDENTIFIER) && (
                        stepsToCalculateValues.contains(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER) ||
                        stepsToCalculateValues.contains(stepElement.getId())
                );

        var _deriveErrors =
                deriveErrors &&
                !stepsToValidate.isEmpty() &&
                !stepsToValidate.contains(FORM_STEP_LIMIT_NONE_IDENTIFIER) && (
                        stepsToValidate.contains(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER) ||
                        stepsToValidate.contains(stepElement.getId())
                );

        super.deriveStepElement(
                context,
                stepElement,
                _deriveVisibilities,
                _deriveOverrides,
                _deriveValues,
                _deriveErrors
        );
    }

    @Override
    protected BaseElementVisibilityDerivationService<FormDerivationContext> getVisibilityDerivationService() {
        return new FormVisibilityDerivationService();
    }

    @Override
    protected BaseElementOverrideDerivationService<FormDerivationContext> getOverrideDerivationService() {
        return new FormOverrideDerivationService();
    }

    @Override
    protected BaseElementValueDerivationService<FormDerivationContext> getValueDerivationService() {
        return new FormValueDerivationService();
    }

    @Override
    protected BaseElementErrorDerivationService<FormDerivationContext> getErrorDerivationService() {
        return new FormErrorDerivationService();
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
