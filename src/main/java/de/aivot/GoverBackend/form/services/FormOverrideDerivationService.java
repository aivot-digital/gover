package de.aivot.GoverBackend.form.services;

import de.aivot.GoverBackend.elements.models.BaseElement;
import de.aivot.GoverBackend.elements.models.steps.StepElement;
import de.aivot.GoverBackend.elements.services.BaseElementOverrideDerivationService;
import de.aivot.GoverBackend.form.models.FormDerivationContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FormOverrideDerivationService extends BaseElementOverrideDerivationService<FormDerivationContext> {

    @Override
    protected void deriveOverridesForStepElement(
            @Nonnull FormDerivationContext context,
            @Nullable String idPrefix,
            @Nonnull StepElement stepElement,
            @Nonnull Boolean skipChildren
    ) {
        boolean needsDerivation = (
                context.getStepsToCalculateOverrides().contains(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER) ||
                context.getStepsToCalculateOverrides().contains(stepElement.getId()) ||
                !stepElement.getOverrideReferencedIds().isEmpty()
        );

        boolean isNotMarkedForExplicitCalculation = !(
                context.getStepsToCalculateOverrides().contains(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER) ||
                context.getStepsToCalculateOverrides().contains(stepElement.getId())
        );

        if (needsDerivation) {
            super.deriveOverridesForStepElement(context, idPrefix, stepElement, skipChildren || isNotMarkedForExplicitCalculation);
        }
    }
}
