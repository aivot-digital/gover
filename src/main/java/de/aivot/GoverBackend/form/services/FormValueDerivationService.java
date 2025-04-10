package de.aivot.GoverBackend.form.services;

import de.aivot.GoverBackend.elements.models.steps.StepElement;
import de.aivot.GoverBackend.elements.services.BaseElementValueDerivationService;
import de.aivot.GoverBackend.form.models.FormDerivationContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FormValueDerivationService extends BaseElementValueDerivationService<FormDerivationContext> {
    @Override
    protected void deriveValuesForStepElement(
            @Nonnull FormDerivationContext context,
            @Nullable String idPrefix,
            @Nonnull StepElement stepElement
    ) {
        if (context.getStepsToCalculateValues().contains(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER) || context.getStepsToCalculateValues().contains(stepElement.getId())) {
            super.deriveValuesForStepElement(context, idPrefix, stepElement);
        }
    }
}
