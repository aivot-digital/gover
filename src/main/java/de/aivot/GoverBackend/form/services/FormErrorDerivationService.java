package de.aivot.GoverBackend.form.services;

public class FormErrorDerivationService {
/*
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
 */
}
