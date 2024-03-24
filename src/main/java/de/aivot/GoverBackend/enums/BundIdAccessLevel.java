package de.aivot.GoverBackend.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.GoverBackend.lib.Identifiable;

public enum BundIdAccessLevel implements Identifiable<String> {
    OPTIONAL("STORK-QAA-Level-0", 0),
    LOW("STORK-QAA-Level-1", 1),
    MEDIUM("STORK-QAA-Level-3", 3),
    HIGH("STORK-QAA-Level-4", 4);

    private final String bundIdStorkIdentifier;
    private final Integer accessRank;

    private BundIdAccessLevel(String bundIdStorkIdentifier, Integer accessRank) {
        this.bundIdStorkIdentifier = bundIdStorkIdentifier;
        this.accessRank = accessRank;
    }


    @Override
    @JsonValue
    public String getKey() {
        return bundIdStorkIdentifier;
    }

    @Override
    public boolean matches(Object other) {
        if (other instanceof String) {
            return bundIdStorkIdentifier.equals(other);
        } else if (other instanceof BundIdAccessLevel accessLevel) {
            return bundIdStorkIdentifier.equals(accessLevel.bundIdStorkIdentifier);
        }
        return false;
    }

    public boolean isHigherThan(BundIdAccessLevel other) {
        if (other == null) {
            return false;
        }
        return accessRank > other.accessRank;
    }

    public boolean isLowerThan(BundIdAccessLevel other) {
        if (other == null) {
            return false;
        }
        return accessRank < other.accessRank;
    }

    public String getBundIdStorkIdentifier() {
        return bundIdStorkIdentifier;
    }

    public Integer getAccessRank() {
        return accessRank;
    }


}
