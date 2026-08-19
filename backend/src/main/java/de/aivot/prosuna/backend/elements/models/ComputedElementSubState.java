package de.aivot.prosuna.backend.elements.models;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Objects;

public class ComputedElementSubState implements Serializable {
    @Nullable
    private String id = null;

    @Nonnull
    private ComputedElementStates states = new ComputedElementStates();

    public ComputedElementSubState() {
    }

    public ComputedElementSubState(@Nullable String id, @Nonnull ComputedElementStates states) {
        this.id = id;
        this.states = states;
    }

    public static ComputedElementSubState of(@Nullable String id, @Nonnull ComputedElementStates states) {
        return new ComputedElementSubState(id, states);
    }

    @JsonAnySetter
    public void setLegacyState(@Nonnull String elementId, @Nonnull ComputedElementState state) {
        states.put(elementId, state);
    }

    @Nullable
    public String getId() {
        return id;
    }

    public ComputedElementSubState setId(@Nullable String id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public ComputedElementStates getStates() {
        return states;
    }

    public ComputedElementSubState setStates(@Nullable ComputedElementStates states) {
        this.states = states != null ? states : new ComputedElementStates();
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ComputedElementSubState that = (ComputedElementSubState) o;
        return Objects.equals(id, that.id) && Objects.equals(states, that.states);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, states);
    }
}
