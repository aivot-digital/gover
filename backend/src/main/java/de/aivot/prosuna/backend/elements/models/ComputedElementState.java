package de.aivot.prosuna.backend.elements.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.aivot.prosuna.backend.elements.enums.EffectiveValueSource;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
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

    /**
     * Indicates that an authored dynamic value was structurally valid but could not be evaluated without runtime
     * process data. This server-side state lets configuration validators distinguish it from a missing literal.
     */
    private boolean inputValueDeferred = false;

    /**
     * Indicates that the input takes its effective value from the authored values of the derivation request. Hidden
     * inputs, protected inputs that take trusted values, computed values and rows computed on the server do not.
     * Callers use it to persist only the request data that the derivation actually took, for example uploaded files.
     */
    private boolean authoredValueAdopted = false;

    /**
     * Indicates that the error comes from deriving the element itself, for example its visibility or value, and not
     * from validation. Every derivation reports such an error again, even with validation skipped, so clients must not
     * keep it once a later derivation no longer reports it.
     */
    private boolean derivationFailed = false;

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
                valueSource == that.valueSource && inputValueDeferred == that.inputValueDeferred &&
                authoredValueAdopted == that.authoredValueAdopted && derivationFailed == that.derivationFailed &&
                Objects.equals(subStates, that.subStates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(visible, disabled, error, errorDetails, override, valueSource, inputValueDeferred, authoredValueAdopted, derivationFailed, subStates);
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

    /** The flag is an internal derivation detail and is not part of the element-state API consumed by clients. */
    @JsonIgnore
    public boolean isInputValueDeferred() {
        return inputValueDeferred;
    }

    public ComputedElementState setInputValueDeferred(boolean inputValueDeferred) {
        this.inputValueDeferred = inputValueDeferred;
        return this;
    }

    /** The flag is an internal derivation detail and is not part of the element-state API consumed by clients. */
    @JsonIgnore
    public boolean isAuthoredValueAdopted() {
        return authoredValueAdopted;
    }

    public ComputedElementState setAuthoredValueAdopted(boolean authoredValueAdopted) {
        this.authoredValueAdopted = authoredValueAdopted;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public boolean isDerivationFailed() {
        return derivationFailed;
    }

    public ComputedElementState setDerivationFailed(boolean derivationFailed) {
        this.derivationFailed = derivationFailed;
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
