package de.aivot.prosuna.backend.elements.models.elements.layout;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;

public class ReplicatingContainerLayoutElementValue {
    private String id;
    private AuthoredElementValues values;

    public String getId() {
        return id;
    }

    public ReplicatingContainerLayoutElementValue setId(String id) {
        this.id = id;
        return this;
    }

    public AuthoredElementValues getValues() {
        return values;
    }

    public ReplicatingContainerLayoutElementValue setValues(AuthoredElementValues values) {
        this.values = values;
        return this;
    }
}
