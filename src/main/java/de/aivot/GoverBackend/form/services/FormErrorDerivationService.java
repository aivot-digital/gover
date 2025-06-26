package de.aivot.GoverBackend.form.services;

import de.aivot.GoverBackend.elements.models.BaseElement;
import de.aivot.GoverBackend.elements.services.BaseElementErrorDerivationService;
import de.aivot.GoverBackend.form.models.FormDerivationContext;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.elements.models.steps.IntroductionStepElement;
import de.aivot.GoverBackend.elements.models.steps.StepElement;
import de.aivot.GoverBackend.elements.models.steps.SubmitStepElement;
import de.aivot.GoverBackend.elements.models.steps.SummaryStepElement;
import de.aivot.GoverBackend.identity.constants.IdentityValueKey;
import de.aivot.GoverBackend.identity.models.IdentityValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

public class FormErrorDerivationService extends BaseElementErrorDerivationService<FormDerivationContext> {

    @Override
    public void deriveErrorForElement(
            @Nonnull FormDerivationContext context,
            @Nullable String idPrefix,
            @Nonnull BaseElement originalElement
    ) {
        if (context.getStepsToValidate().contains(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER)) {
            switch (originalElement) {
                case IntroductionStepElement introductionStepElement -> {
                    deriveErrorsForIntroductionStep(context, introductionStepElement);
                }
                case SummaryStepElement summaryStepElement -> {
                    deriveErrorsForSummaryStep(context, summaryStepElement);
                }
                case SubmitStepElement submitStepElement -> {
                    deriveErrorsForSubmitStep(context, submitStepElement);
                }
                default -> {
                    // No specific error derivation for other elements
                }
            }
        }

        else if (
                originalElement instanceof IntroductionStepElement introductionStepElement &&
                context.getStepsToValidate().contains(introductionStepElement.getId())
        ) {
            deriveErrorsForIntroductionStep(context, introductionStepElement);
        }

        else if (
                originalElement instanceof SummaryStepElement summaryStepElement &&
                context.getStepsToValidate().contains(summaryStepElement.getId())
        ) {
            deriveErrorsForSummaryStep(context, summaryStepElement);
        }

        else if (
                originalElement instanceof SubmitStepElement submitStepElement &&
                context.getStepsToValidate().contains(submitStepElement.getId())
        ) {
            deriveErrorsForSubmitStep(context, submitStepElement);
        }

        else {
            super.deriveErrorForElement(context, idPrefix, originalElement);
        }
    }

    private void deriveErrorsForIntroductionStep(FormDerivationContext context, IntroductionStepElement introductionStep) {
        var derivationData = context
                .getElementDerivationData();

        // Check if privacy flag is set to true in the customer input and if not, set error message
        if (!derivationData.getValue(IntroductionStepElement.PRIVACY_CHECKBOX_ID, Boolean.class).orElse(false)) {
            derivationData.setError(IntroductionStepElement.PRIVACY_CHECKBOX_ID, "Bitte akzeptieren Sie die Hinweise zum Datenschutz.");
        }

        // Check if the form requires a customer id and if it is, check if the customer id is set in the customer input and if not, set error message
        var form = context.getForm();

        if (form.getIdentityRequired()) {
            var identityData = derivationData
                    .getValue(IdentityValueKey.IdCustomerInputKey, Map.class);

            if (identityData.isEmpty()) {
                derivationData
                        .setError(
                                IdentityValueKey.IdCustomerInputKey,
                                "Bitte melden Sie sich mit einem der bereitgestellten Konten an."
                        );
            } else {
                var identityValue = IdentityValue.fromMap(identityData.get());
                if (identityValue.isEmpty()) {
                    derivationData
                            .setError(
                                    IdentityValueKey.IdCustomerInputKey,
                                    "Bitte melden Sie sich mit einem der bereitgestellten Konten an."
                            );
                }
            }
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
        var derivationData = context.getElementDerivationData();

        Optional<String> captchaRawValue;
        try {
            captchaRawValue = derivationData
                    .getValue(SubmitStepElement.CAPTCHA_FIELD_ID, String.class);
        } catch (ClassCastException e) {
            captchaRawValue = Optional.empty();
        }

        if (captchaRawValue.isEmpty()) {
            derivationData.setError(
                    SubmitStepElement.CAPTCHA_FIELD_ID,
                    "Bitte bestätigen Sie, dass Sie ein Mensch sind."
            );
            return;
        }

        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(captchaRawValue.get());

            var payloadNode = node.get("payload");
            var expiresNode = node.get("expiresAt");

            if (payloadNode == null || payloadNode.isNull() || payloadNode.asText().isBlank()) {
                derivationData.setError(
                        SubmitStepElement.CAPTCHA_FIELD_ID,
                        "Bitte bestätigen Sie, dass Sie ein Mensch sind."
                );
                return;
            }

            // check expiration
            if (expiresNode != null && expiresNode.isNumber()) {
                long now = java.time.Instant.now().getEpochSecond();
                long expiresAt = expiresNode.asLong();

                if (expiresAt < now) {
                    derivationData.setError(
                            SubmitStepElement.CAPTCHA_FIELD_ID,
                            "Die Captcha-Bestätigung ist abgelaufen. Bitte erneut bestätigen."
                    );
                    return;
                }
            }

        } catch (Exception e) {
            // Payload not parseable → invalid
            derivationData.setError(
                    SubmitStepElement.CAPTCHA_FIELD_ID,
                    "Die Captcha-Daten konnten nicht gelesen werden. Bitte erneut versuchen."
            );
        }
    }
}
