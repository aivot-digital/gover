package de.aivot.gover.backend.elements.models.elements.form.input;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.gover.backend.exceptions.ValidationException;
import de.aivot.gover.backend.utils.StringUtils;
import jakarta.annotation.Nullable;

import java.util.regex.Pattern;

public record PaymentConfigElementValueRequestorMapping(
        @Nullable
        RequestorSourceType requestorSourceType,

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
        @JsonAlias("streeDestinationKey")
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
    private static final Pattern PROCESS_DATA_KEY_PATTERN = Pattern.compile("[a-zA-Z0-9._]+");

    public void performValidation() throws ValidationException {
        if (requestorSourceType == null) {
            throw new ValidationException(null, "Es muss eine Quelle für die Antragsteller-Zuweisung ausgewählt werden.");
        }

        validateDestinationKeys();

        switch (requestorSourceType) {
            case FixPerson -> {
                requireDestinationKey(lastNameDestinationKey, "Nachname");
                requireDestinationKey(firstNameDestinationKey, "Vorname");
                requireDestinationKey(genderDestinationKey, "Geschlecht");
                requireAddress();
            }
            case FixOrg -> {
                requireDestinationKey(organizationNameDestinationKey, "Organisationsname");
                requireAddress();
            }
            case ProcessDataKey -> {
                requireDestinationKey(isOrganizationDestinationKey, "Ist Organisation");
                requireDestinationKey(lastNameDestinationKey, "Nachname");
                requireDestinationKey(firstNameDestinationKey, "Vorname");
                requireDestinationKey(genderDestinationKey, "Geschlecht");
                requireDestinationKey(organizationNameDestinationKey, "Organisationsname");
                requireAddress();
            }
        }
    }

    private void validateDestinationKeys() throws ValidationException {
        validateDestinationKey(lastNameDestinationKey, "Nachname");
        validateDestinationKey(firstNameDestinationKey, "Vorname");
        validateDestinationKey(genderDestinationKey, "Geschlecht");
        validateDestinationKey(isOrganizationDestinationKey, "Ist Organisation");
        validateDestinationKey(organizationNameDestinationKey, "Organisationsname");
        validateDestinationKey(streetDestinationKey, "Straße");
        validateDestinationKey(houseNumberDestinationKey, "Hausnummer");
        validateDestinationKey(addressLineDestinationKey, "Adresszusatz");
        validateDestinationKey(postalCodeDestinationKey, "Postleitzahl");
        validateDestinationKey(cityDestinationKey, "Ort");
        validateDestinationKey(countryDestinationKey, "Land");
    }

    private void validateDestinationKey(@Nullable String destinationKey, String label) throws ValidationException {
        if (StringUtils.isNullOrEmpty(destinationKey)) {
            return;
        }

        if (!PROCESS_DATA_KEY_PATTERN.matcher(destinationKey).matches()) {
            throw new ValidationException(null, label + " muss ein gültiger Prozessdaten-Schlüssel sein.");
        }
    }

    private void requireDestinationKey(@Nullable String destinationKey, String label) throws ValidationException {
        if (StringUtils.isNullOrEmpty(destinationKey)) {
            throw new ValidationException(null, label + " muss zugewiesen werden.");
        }
    }

    private void requireAddress() throws ValidationException {
        requireDestinationKey(streetDestinationKey, "Straße");
        requireDestinationKey(houseNumberDestinationKey, "Hausnummer");
        requireDestinationKey(addressLineDestinationKey, "Adresszusatz");
        requireDestinationKey(postalCodeDestinationKey, "Postleitzahl");
        requireDestinationKey(cityDestinationKey, "Ort");
        requireDestinationKey(countryDestinationKey, "Land");
    }

    public enum RequestorSourceType {
        FixPerson("fixPerson"),
        FixOrg("fixOrg"),
        ProcessDataKey("processDataKey");

        private final String key;

        RequestorSourceType(String key) {
            this.key = key;
        }

        @JsonValue
        public String getKey() {
            return key;
        }

        @JsonCreator
        @Nullable
        public static RequestorSourceType fromKey(@Nullable String key) {
            for (var value : values()) {
                if (value.key.equals(key) || value.name().equals(key)) {
                    return value;
                }
            }

            return null;
        }
    }
}
