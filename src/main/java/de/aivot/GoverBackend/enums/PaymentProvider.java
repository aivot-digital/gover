package de.aivot.GoverBackend.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.GoverBackend.lib.models.Identifiable;

public enum PaymentProvider implements Identifiable<String> {
    ePayBL("epaybl", "ePayBL"),
    pmPayment("pmpayment", "pmPayment"),
    giroPay("giropay", "giropay"),
    ;

    private final String key;
    private final String label;

    PaymentProvider(String key, String label) {
        this.key = key;
        this.label = label;
    }

    @Override
    @JsonValue
    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean matches(Object other) {
        return key.equals(other);
    }
}
