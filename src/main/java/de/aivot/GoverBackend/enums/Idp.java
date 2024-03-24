package de.aivot.GoverBackend.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.GoverBackend.lib.Identifiable;

import java.util.Optional;

public enum Idp implements Identifiable<String> {
    BAYERN_ID("bayern_id", "bayernIdMapping"),
    BUND_ID("bund_id", "bundIdMapping"),
    MUK("muk", "mukMapping"),
    SH_ID("sh_id", "shIdMapping");

    private final String identifier;
    private final String mappingSource;

    private Idp(String identifier, String mappingSource) {
        this.identifier = identifier;
        this.mappingSource = mappingSource;
    }


    @Override
    @JsonValue
    public String getKey() {
        return identifier;
    }

    @Override
    public boolean matches(Object other) {
        if (other instanceof String) {
            return identifier.equals(other);
        } else if (other instanceof Idp idp) {
            return identifier.equals(idp.identifier);
        }
        return false;
    }

    public static Optional<Idp> fromString(String idp) {
        for (Idp value : values()) {
            if (value.identifier.equals(idp)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    public String getMappingSource() {
        return mappingSource;
    }
}
