package de.aivot.gover.backend.elements.models.elements.form.input;

import de.aivot.gover.backend.exceptions.ValidationException;
import jakarta.annotation.Nullable;

public record PaymentConfigElementValueRequestorMapping(
        @Nullable
        String lastNameDestinationKey,
        @Nullable
        String firstNameDestinationKey,
        @Nullable
        String genderDestinationKey,

        // Org
        @Nullable
        String isOrganizationDestinationKey,
        @Nullable
        String organizationNameDestinationKey,

        // Adresse
        @Nullable
        String streetDestinationKey,
        @Nullable
        String houseNumberDestinationKey,
        @Nullable
        String addressLineDestinationKey,
        @Nullable
        String postalCodeDestinationKey,
        @Nullable
        String cityDestinationKey,
        @Nullable
        String countryDestinationKey
) {
    public void performValidation() throws ValidationException {
         // TODO
    }
}
