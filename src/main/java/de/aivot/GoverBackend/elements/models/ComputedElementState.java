package de.aivot.GoverBackend.elements.models;

import de.aivot.GoverBackend.elements.enums.EffectiveValueSource;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class ComputedElementState implements Serializable {
    @Nonnull
    private Boolean visible = true;

    @Nullable
    private String error = null;

    @Nullable
    private BaseElement override = null;

    @Nonnull
    private EffectiveValueSource valueSource = EffectiveValueSource.Authored;

    @Nullable
    private List<ComputedElementStates> subStates = null;

    // region Constructors

    public ComputedElementState() {

    }

    public ComputedElementState(@Nonnull Boolean visible,
                                @Nullable String error,
                                @Nullable BaseElement override,
                                @Nonnull EffectiveValueSource valueSource,
                                @Nullable List<ComputedElementStates> subStates) {
        this.visible = visible;
        this.error = error;
        this.override = override;
        this.valueSource = valueSource;
        this.subStates = subStates;
    }

    public static ComputedElementState create() {
        return new ComputedElementState();
    }

    // endregion

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ComputedElementState that = (ComputedElementState) o;
        return Objects.equals(visible, that.visible) && Objects.equals(error, that.error) && Objects.equals(override, that.override) && valueSource == that.valueSource && Objects.equals(subStates, that.subStates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(visible, error, override, valueSource, subStates);
    }

    // endregion

    // region Getters & Setters

    @Nonnull
    public Boolean getVisible() {
        return visible;
    }

    public ComputedElementState setVisible(@Nonnull Boolean visible) {
        this.visible = visible;
        return this;
    }

    @Nullable
    public String getError() {
        return error;
    }

    public ComputedElementState setError(@Nullable String error) {
        this.error = error;
        return this;
    }

    @Nullable
    public BaseElement getOverride() {
        return override;
    }

    public ComputedElementState setOverride(@Nullable BaseElement override) {
        this.override = override;
        return this;
    }

    @Nonnull
    public EffectiveValueSource getValueSource() {
        return valueSource;
    }

    public ComputedElementState setValueSource(@Nonnull EffectiveValueSource valueSource) {
        this.valueSource = valueSource;
        return this;
    }

    @Nullable
    public List<ComputedElementStates> getSubStates() {
        return subStates;
    }

    public ComputedElementState setSubStates(@Nullable List<ComputedElementStates> subStates) {
        this.subStates = subStates;
        return this;
    }

    // endregion
}
