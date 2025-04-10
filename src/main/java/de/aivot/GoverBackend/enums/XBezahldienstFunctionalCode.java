package de.aivot.GoverBackend.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.GoverBackend.lib.models.Identifiable;

/**
 * Die genauen Ausprägungen der Funktionalen Response müssen noch diskutiert werden. Arbeitsthese:
 */
public enum XBezahldienstFunctionalCode implements Identifiable<String> {
    NO_VALID_ORIGINATOR_OR_DESTINATION("NO_VALID_ORIGINATOR_OR_DESTINATION"),
    NO_VALID_ORIGINATOR_OR_ENDPOINT("NO_VALID_ORIGINATOR_OR_ENDPOINT"),

    // PAYMENT_EXISTS - Mit dieser Transaktions-ID wurde bereits ein PaymentRequest platziert.
    PAYMENT_EXISTS("PAYMENT_EXISTS"),

    // CALCULATION_ERROR - Die Summe des PaymentRequests passt nicht zu den Positionen.
    CALCULATION_ERROR("CALCULATION_ERROR"),

    // BOOKING_DATA_MISSING - Notwendige Daten für die Verbuchung fehlen - genaue Beschreibung im functionalText.
    BOOKING_DATA_MISSING("BOOKING_DATA_MISSING"),
    VALIDATION_ERROR("VALIDATION_ERROR"),

    // UNSPECIFIED - Alle Fehler, die hier aktuell nicht konkret definiert sind.
    UNSPECIFIED("UNSPECIFIED"),

    // FORBIDDEN - Fehlende Authentisierungsdaten oder keine Authorisierung in diesem Kontext.
    FORBIDDEN("FORBIDDEN");

    private final String key;

    XBezahldienstFunctionalCode(String key) {
        this.key = key;
    }

    @Override
    @JsonValue
    public String getKey() {
        return key;
    }

    @Override
    public boolean matches(Object other) {
        return key.equals(other);
    }
}
