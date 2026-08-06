package de.aivot.gover.backend.elements.models;

import de.aivot.gover.backend.elements.enums.EffectiveValueSource;
import de.aivot.gover.backend.elements.models.elements.BaseElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class ComputedElementState implements Serializable {
    @Nonnull
    private Boolean visible = true;

    @Nonnull
    private Boolean disabled = false;

    @Nullable
    private String error = null;

    @Nullable
    private Object errorDetails = null;

    @Nullable
    private BaseElement override = null;

    @Nonnull
    private EffectiveValueSource valueSource = EffectiveValueSource.Authored;

    @Nullable
    private List<ComputedElementSubState> subStates = null;

    // region Constructors

    public ComputedElementState() {

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
        return Objects.equals(visible, that.visible) && Objects.equals(disabled, that.disabled) && Objects.equals(error, that.error) &&
                Objects.equals(errorDetails, that.errorDetails) && Objects.equals(override, that.override) &&
                valueSource == that.valueSource && Objects.equals(subStates, that.subStates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(visible, disabled, error, errorDetails, override, valueSource, subStates);
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

    @Nonnull
    public Boolean getDisabled() {
        return disabled;
    }

    public ComputedElementState setDisabled(@Nonnull Boolean disabled) {
        this.disabled = disabled;
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
    public List<ComputedElementSubState> getSubStates() {
        return subStates;
    }

    public ComputedElementState setSubStates(@Nullable List<ComputedElementSubState> subStates) {
        this.subStates = subStates;
        return this;
    }

    @Nullable
    public Object getErrorDetails() {
        return errorDetails;
    }

    public ComputedElementState setErrorDetails(@Nullable Object errorDetails) {
        this.errorDetails = errorDetails;
        return this;
    }

    // endregion
}
