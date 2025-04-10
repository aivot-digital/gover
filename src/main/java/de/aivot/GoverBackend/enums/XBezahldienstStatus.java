package de.aivot.GoverBackend.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.GoverBackend.lib.models.Identifiable;

public enum XBezahldienstStatus implements Identifiable<String> {
    // INITIAL - der Antrag hat einen Payment-Request ausgelöst und eine Payment-Transaction wurde angelegt.
    // Der Nutzer hat aber im Bezahldienst noch keine Wirkung erzeugt.
    // Der Benutzer kann mit dem Link zum Bezahldienst weitergeleitet werden.
    INITIAL("INITIAL"),
    // PAYED - der Nutzer hat die Bezahlung im Bezahldienst erfolgreich durchgeführt.
    PAYED("PAYED"),
    // FAILED - der Vorgang wurde vom Bezahldienst aufgrund der Nutzereingaben abgebrochen.
    FAILED("FAILED"),
    // CANCELED - der Nutzer hat die Bezahlung im Bezahldienst abgebrochen.
    CANCELED("CANCELED");

    private final String key;

    XBezahldienstStatus(String key) {
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
