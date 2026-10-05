package de.aivot.prosuna.backend.elements.models.elements.layout;

import de.aivot.prosuna.backend.elements.models.EffectiveElementValues;

/** A derived container row whose nested values no longer contain authored input-mode envelopes. */
public class EffectiveReplicatingContainerLayoutElementValue {
    private String id;
    private EffectiveElementValues values;

    public String getId() {
        return id;
    }

    public EffectiveReplicatingContainerLayoutElementValue setId(String id) {
        this.id = id;
        return this;
    }

    public EffectiveElementValues getValues() {
        return values;
    }

    public EffectiveReplicatingContainerLayoutElementValue setValues(EffectiveElementValues values) {
        this.values = values;
        return this;
    }
}
