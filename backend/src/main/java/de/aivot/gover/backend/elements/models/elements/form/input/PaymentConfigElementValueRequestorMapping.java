package de.aivot.gover.backend.elements.models.elements.form.input;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.gover.backend.utils.StringUtils;
import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
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

    public Map<String, Object> performValidation() {
        var errors = new HashMap<String, Object>();

        validateDestinationKeys(errors);

        if (requestorSourceType == null) {
            errors.put("requestorSourceType", "Es muss eine Quelle für die Antragsteller-Zuweisung ausgewählt werden.");
            return errors;
        }

        if (requestorSourceType == RequestorSourceType.ProcessDataKey) {
            requireDestinationKey(errors, "isOrganizationDestinationKey", isOrganizationDestinationKey, "Ist Organisation");
        }

        return errors;
    }

    private void validateDestinationKeys(Map<String, Object> errors) {
        validateDestinationKey(errors, "lastNameDestinationKey", lastNameDestinationKey, "Nachname");
        validateDestinationKey(errors, "firstNameDestinationKey", firstNameDestinationKey, "Vorname");
        validateDestinationKey(errors, "genderDestinationKey", genderDestinationKey, "Geschlecht");
        validateDestinationKey(errors, "isOrganizationDestinationKey", isOrganizationDestinationKey, "Ist Organisation");
        validateDestinationKey(errors, "organizationNameDestinationKey", organizationNameDestinationKey, "Organisationsname");
        validateDestinationKey(errors, "streetDestinationKey", streetDestinationKey, "Straße");
        validateDestinationKey(errors, "houseNumberDestinationKey", houseNumberDestinationKey, "Hausnummer");
        validateDestinationKey(errors, "addressLineDestinationKey", addressLineDestinationKey, "Adresszusatz");
        validateDestinationKey(errors, "postalCodeDestinationKey", postalCodeDestinationKey, "Postleitzahl");
        validateDestinationKey(errors, "cityDestinationKey", cityDestinationKey, "Ort");
        validateDestinationKey(errors, "countryDestinationKey", countryDestinationKey, "Land");
    }

    private void validateDestinationKey(Map<String, Object> errors, String fieldKey, @Nullable String destinationKey, String label) {
        if (StringUtils.isNullOrEmpty(destinationKey)) {
            return;
        }

        if (!PROCESS_DATA_KEY_PATTERN.matcher(destinationKey).matches()) {
            errors.put(fieldKey, label + " muss ein gültiger Prozessdaten-Schlüssel sein.");
        }
    }

    private void requireDestinationKey(Map<String, Object> errors, String fieldKey, @Nullable String destinationKey, String label) {
        if (StringUtils.isNullOrEmpty(destinationKey)) {
            errors.put(fieldKey, label + " muss zugewiesen werden.");
        }
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
