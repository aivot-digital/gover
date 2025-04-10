package de.aivot.GoverBackend.form.services;

import de.aivot.GoverBackend.elements.models.BaseElement;
import de.aivot.GoverBackend.elements.models.steps.StepElement;
import de.aivot.GoverBackend.elements.services.BaseElementVisibilityDerivationService;
import de.aivot.GoverBackend.form.models.FormDerivationContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FormVisibilityDerivationService extends BaseElementVisibilityDerivationService<FormDerivationContext> {
    @Override
    protected void deriveVisibilitiesForStepElement(
            @Nonnull FormDerivationContext context,
            @Nullable String idPrefix,
            @Nonnull StepElement stepElement,
            @Nonnull Boolean isParentVisible,
            @Nonnull Boolean skipChildren
    ) {
        boolean needsDerivation = (
                context.getStepsToCalculateVisibilities().contains(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER) ||
                context.getStepsToCalculateVisibilities().contains(stepElement.getId()) ||
                !stepElement.getVisibilityReferencedIds().isEmpty()
        );

        boolean isNotMarkedForExplicitCalculation = !(
                context.getStepsToCalculateVisibilities().contains(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER) ||
                context.getStepsToCalculateVisibilities().contains(stepElement.getId())
        );

        if (needsDerivation) {
            super.deriveVisibilitiesForStepElement(context, idPrefix, stepElement, isParentVisible, skipChildren || isNotMarkedForExplicitCalculation);
        }
    }

    @Override
    protected boolean deriveVisibilityForBaseElement(
            @Nonnull FormDerivationContext context,
            @Nullable String idPrefix,
            @Nonnull BaseElement baseElement,
            @Nonnull Boolean isParentVisible
    ) {
        return super.deriveVisibilityForBaseElement(context, idPrefix, baseElement, isParentVisible);
    }
}
