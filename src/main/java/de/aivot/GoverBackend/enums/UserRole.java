package de.aivot.GoverBackend.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.GoverBackend.lib.Identifiable;

public enum UserRole implements Identifiable<Integer> {
    Editor(0, "Bearbeiter:in"),
    Publisher(1, "Veröffentlicher:in"),
    Admin(2, "Administrator:in");

    private final Integer key;
    private final String label;

    private UserRole(Integer key, String label) {
        this.key = key;
        this.label = label;
    }

    @Override
    @JsonValue
    public Integer getKey() {
        return key;
    }

    @Override
    public boolean matches(Object other) {
        return key.equals(other);
    }

    public String getLabel() {
        return label;
    }
}
