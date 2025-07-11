package de.aivot.GoverBackend.form.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.form.entities.Form;
import de.aivot.GoverBackend.identity.constants.IdentityValueKey;
import de.aivot.GoverBackend.identity.models.IdentityValue;
import jakarta.annotation.Nonnull;

import java.util.Optional;

public class FormIdentityCheckService {
    @Nonnull
    private Optional<String> deriveErrorsForIntroductionStep(Form form, ElementData elementData) {
        if (!form.getIdentityRequired()) {
            return Optional.empty();
        }

        var identityData = elementData
                .get(IdentityValueKey.IdCustomerInputKey);

        if (identityData == null || identityData.getValue() == null) {
            return Optional.of("Bitte melden Sie sich mit einem der bereitgestellten Konten an.");
        } else {
            var identityValue = new ObjectMapper()
                    .convertValue(identityData.getValue(), IdentityValue.class);

            if (identityValue.isEmpty()) {
                return Optional.of("Bitte melden Sie sich mit einem der bereitgestellten Konten an.");
            }
        }

        return Optional.empty();
    }
}
