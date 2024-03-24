package de.aivot.GoverBackend.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.GoverBackend.lib.Identifiable;

public enum MukAccessLevel implements Identifiable<String> {
    OPTIONAL("optional", 0),
    REQUIRED("required", 1),
    ;

    private final String mukIdentifier;
    private final Integer accessRank;

    private MukAccessLevel(String mukIdentifier, Integer accessRank) {
        this.mukIdentifier = mukIdentifier;
        this.accessRank = accessRank;
    }


    @Override
    @JsonValue
    public String getKey() {
        return mukIdentifier;
    }

    @Override
    public boolean matches(Object other) {
        if (other instanceof String) {
            return mukIdentifier.equals(other);
        } else if (other instanceof MukAccessLevel accessLevel) {
            return mukIdentifier.equals(accessLevel.mukIdentifier);
        }
        return false;
    }

    public boolean isHigherThan(MukAccessLevel other) {
        if (other == null) {
            return false;
        }
        return accessRank > other.accessRank;
    }

    public boolean isLowerThan(MukAccessLevel other) {
        if (other == null) {
            return false;
        }
        return accessRank < other.accessRank;
    }

    public String getMukIdentifier() {
        return mukIdentifier;
    }

    public Integer getAccessRank() {
        return accessRank;
    }


}
