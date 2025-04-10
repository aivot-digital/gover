package de.aivot.GoverBackend.form.services;

import de.aivot.GoverBackend.elements.services.BaseElementErrorDerivationService;
import de.aivot.GoverBackend.enums.BayernIdAccessLevel;
import de.aivot.GoverBackend.enums.BundIdAccessLevel;
import de.aivot.GoverBackend.enums.MukAccessLevel;
import de.aivot.GoverBackend.enums.SchleswigHolsteinIdAccessLevel;
import de.aivot.GoverBackend.form.models.FormDerivationContext;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.elements.models.steps.IntroductionStepElement;
import de.aivot.GoverBackend.elements.models.steps.StepElement;
import de.aivot.GoverBackend.elements.models.steps.SubmitStepElement;
import de.aivot.GoverBackend.elements.models.steps.SummaryStepElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

public class FormErrorDerivationService extends BaseElementErrorDerivationService<FormDerivationContext> {
    @Override
    protected void deriveErrorsForRootElement(
            @Nonnull FormDerivationContext context,
            @Nullable String idPrefix,
            @Nonnull RootElement rootElement
    ) {
        if (context.getStepsToValidate().isEmpty() || context.getStepsToValidate().contains(FormDerivationService.FORM_STEP_LIMIT_NONE_IDENTIFIER)) {
            return;
        }

        if (context.getStepsToValidate().contains(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER)) {
            deriveErrorsForIntroductionStep(context, rootElement.getIntroductionStep());
            deriveErrorsForSummaryStep(context, rootElement.getSummaryStep());
            deriveErrorsForSubmitStep(context, rootElement.getSubmitStep());
        } else {
            if (context.getStepsToValidate().contains(rootElement.getIntroductionStep().getId())) {
                deriveErrorsForIntroductionStep(context, rootElement.getIntroductionStep());
            }

            if (context.getStepsToValidate().contains(rootElement.getSummaryStep().getId())) {
                deriveErrorsForSummaryStep(context, rootElement.getSummaryStep());
            }

            if (context.getStepsToValidate().contains(rootElement.getSubmitStep().getId())) {
                deriveErrorsForSubmitStep(context, rootElement.getSubmitStep());
            }
        }

        super.deriveErrorsForRootElement(context, idPrefix, rootElement);
    }

    @Override
    protected void deriveErrorsForStepElement(
            @Nonnull FormDerivationContext context,
            @Nullable String idPrefix,
            @Nonnull StepElement stepElement
    ) {
        boolean needsDerivation = (
                context.getStepsToValidate().contains(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER) ||
                context.getStepsToValidate().contains(stepElement.getId())
        );

        if (needsDerivation) {
            super.deriveErrorsForStepElement(context, idPrefix, stepElement);
        }
    }

    private void deriveErrorsForIntroductionStep(FormDerivationContext context, IntroductionStepElement introductionStep) {
        // Check if privacy flag is set to true in the customer input and if not, set error message
        if (!context.getValue(IntroductionStepElement.PRIVACY_CHECKBOX_ID, Boolean.class).orElse(false)) {
            context.setError(IntroductionStepElement.PRIVACY_CHECKBOX_ID, "Bitte akzeptieren Sie die Hinweise zum Datenschutz.");
        }

        // Check if the form requires a customer id and if it is, check if the customer id is set in the customer input and if not, set error message
        var form = context.getForm();

        var formNeedsAuth = false;
        if (form.getBayernIdEnabled() && form.getBayernIdLevel() != null && form.getBayernIdLevel().isHigherThan(BayernIdAccessLevel.OPTIONAL)) {
            formNeedsAuth = true;
        } else if (form.getBundIdEnabled() && form.getBundIdLevel() != null && form.getBundIdLevel().isHigherThan(BundIdAccessLevel.OPTIONAL)) {
            formNeedsAuth = true;
        } else if (form.getShIdEnabled() && form.getShIdLevel() != null && form.getShIdLevel().isHigherThan(SchleswigHolsteinIdAccessLevel.OPTIONAL)) {
            formNeedsAuth = true;
        } else if (form.getMukEnabled() && form.getMukLevel() != null && form.getMukLevel().isHigherThan(MukAccessLevel.OPTIONAL)) {
            formNeedsAuth = true;
        }

        if (formNeedsAuth && context.getValue(IntroductionStepElement.CUSTOMER_IDENTITY_DATA_ID, Map.class).isEmpty()) {
            context.setError(IntroductionStepElement.CUSTOMER_IDENTITY_DATA_ID, "Bitte melden Sie sich mit einem der bereitgestellten Konten an.");
        }
    }

    private void deriveErrorsForSummaryStep(FormDerivationContext context, SummaryStepElement summaryStep) {
        // Check if flag for confirming the summary is set to true in the customer input and if not, set error message
        if (!context.getValue(SummaryStepElement.SUMMARY_CONFIRMATION, Boolean.class).orElse(false)) {
            context.setError(SummaryStepElement.SUMMARY_CONFIRMATION, "Bitte prüfen und bestätigen Sie die Zusammenfassung.");
        }

        // TODO: The max file size is currently checked in the FormSubmissionController. Should be moved here.
    }

    private void deriveErrorsForSubmitStep(FormDerivationContext context, SubmitStepElement submitStep) {
        if (!context.getValue(SubmitStepElement.CAPTCHA_FILED_ID, Boolean.class).orElse(false)) {
            context.setError(SubmitStepElement.CAPTCHA_FILED_ID, "Bitte bestätigen Sie, dass Sie ein Mensch sind.");
        }
    }
}
