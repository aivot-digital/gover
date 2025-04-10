package de.aivot.GoverBackend.elements.models;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class ElementDerivationData {
    protected final Map<String, Object> inputValues;
    protected final Map<String, Object> computedValues = new HashMap<>();
    protected final Map<String, Boolean> visibilities = new HashMap<>();
    protected final Map<String, String> errors = new HashMap<>();
    protected final Map<String, BaseElement> overrides = new HashMap<>();

    public ElementDerivationData(Map<String, Object> inputValues) {
        this.inputValues = inputValues;
    }

    /**
     * Set the visibility of a field based on the id of the element.
     * Only invisibilities are explicitly set, because all elements are visible by default.
     *
     * @param elementId The id of the element.
     * @param value     The visibility of the element. If the value is null or true, the visibility will be set to true.
     */
    public void setVisibility(@Nonnull String elementId, @Nullable Boolean value) {
        if (value == null || Boolean.TRUE.equals(value)) {
            this.visibilities.put(elementId, true);
        } else {
            this.visibilities.put(elementId, false);
        }
    }

    /**
     * Check if an element is visible based on the id of the element.
     * If the visibility is not set, the method will return true because all elements are visible by default.
     *
     * @param elementId The id of the element.
     * @return The visibility of the element.
     */
    @Nonnull
    public Boolean isVisible(@Nonnull String elementId) {
        return visibilities.getOrDefault(elementId, true);
    }

    /**
     * Check if an element is invisible based on the id of the element.
     * If the visibility is not set, the method will return false because all elements are visible by default.
     *
     * @param elementId The id of the element.
     * @return The invisibility of the element.
     */
    @Nonnull
    public Boolean isInvisible(@Nonnull String elementId) {
        return !isVisible(elementId);
    }

    /**
     * Set the value of a field based on the id of the element.
     *
     * @param elementId The id of the element.
     * @param value     The value of the element.
     */
    public void setValue(@Nonnull String elementId, @Nullable Object value) {
        this.computedValues.put(elementId, value);
    }

    /**
     * Check if a field has a value based on the id of the element.
     * If the element is invisible, the method will return false because invisible elements have no value.
     *
     * @param elementId The id of the element.
     * @return Returns true if the element has a value, otherwise false.
     */
    public boolean hasValue(@Nonnull String elementId) {
        return getValue(elementId).isPresent();
    }

    /**
     * Get the value of a field based on the id of the element.
     * If the element is invisible, the method will return an empty optional because invisible elements have no value.
     * If the value is set by the user, the method will return the user value because user inputs always have priority.
     * There are exceptions for collections and maps. Empty collections and maps are treated as if the user has not entered a value.
     * If the user has not entered a value, the method will check the computed value and return it if it is set.
     * If no value is set at all, the method will return an empty optional.
     *
     * @param elementId The id of the element.
     * @return The value of the element.
     */
    @Nonnull
    public Optional<Object> getValue(@Nonnull String elementId) {
        if (isInvisible(elementId)) {
            return Optional.empty();
        }

        var inputValue = inputValues.get(elementId);
        if (inputValue != null) {
            switch (inputValue) {
                // TODO: Define when a value counts as empty and should be null instead
                case Collection<?> collectionInputValue:
                    if (!collectionInputValue.isEmpty()) {
                        return Optional.of(collectionInputValue);
                    }
                    break;
                case Map<?, ?> mapInputValue:
                    if (!mapInputValue.isEmpty()) {
                        return Optional.of(mapInputValue);
                    }
                    break;
                default:
                    return Optional.of(inputValue);
            }
        }

        var computedValue = computedValues.get(elementId);
        if (computedValue != null) {
            return Optional.of(computedValue);
        }

        return Optional.empty();
    }

    /**
     * Get the value of a field based on the id of the element as a specific class.
     * All logic of the {@link #getValue(String)} method applies here as well.
     * If the value is not of the desired class, the method will throw an {@link ClassCastException}.
     *
     * @param elementId    The id of the element.
     * @param desiredClass The class of the value.
     * @param <T>          The type of the value.
     * @return The value of the element.
     */
    @Nonnull
    public <T> Optional<T> getValue(@Nonnull String elementId, @Nonnull Class<T> desiredClass) {
        var value = getValue(elementId);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        if (!desiredClass.isInstance(value.get())) {
            throw new ClassCastException("Value is not of the expected type. Expected: " + desiredClass + ", Actual: " + value.get().getClass());
        }
        return Optional.of(desiredClass.cast(value.get()));
    }

    /**
     * Set the error of a field based on the id of the element.
     *
     * @param elementId The id of the element.
     * @param error     The error of the element.
     */
    public void setError(@Nonnull String elementId, @Nullable String error) {
        if (error == null) {
            this.errors.remove(elementId);
        } else {
            this.errors.put(elementId, error);
        }
    }

    /**
     * Get the error of a field based on the id of the element.
     * If the error is not set, the method will return an empty optional.
     *
     * @param elementId The id of the element.
     * @return The error of the element.
     */
    @Nonnull
    public Optional<String> getError(@Nonnull String elementId) {
        var error = errors.getOrDefault(elementId, null);
        if (error == null) {
            return Optional.empty();
        }
        return Optional.of(error);
    }

    /**
     * Check if the derivation has produced any errors.
     *
     * @return Returns true if there are any errors, otherwise false.
     */
    @Nonnull
    public Boolean hasErrors() {
        if (errors.isEmpty()) {
            return false;
        }
        return errors.values().stream().anyMatch(Objects::nonNull);
    }

    /**
     * Set the element override of a field based on the id of the element.
     *
     * @param elementId The id of the element.
     * @param override  The element override.
     */
    public void setOverride(@Nonnull String elementId, @Nullable BaseElement override) {
        if (override == null) {
            this.overrides.put(elementId, null);
        } else {
            this.overrides.put(elementId, override);
        }
    }

    /**
     * Get the element override of a field based on the id of the element.
     * If the element override is not set, the method will return an empty optional.
     *
     * @param elementId The id of the element.
     * @return The element override.
     */
    @Nonnull
    public Optional<BaseElement> getOverride(@Nonnull String elementId) {
        var override = overrides.getOrDefault(elementId, null);
        if (override == null) {
            return Optional.empty();
        }
        return Optional.of(override);
    }

    /**
     * Get the element override of a field based on the id of the element.
     * If the element override is not set, the method will return the default value.
     *
     * @param elementId    The id of the element.
     * @param defaultValue The default value.
     * @return The element override.
     */
    @Nonnull
    public BaseElement getOverride(@Nonnull String elementId, @Nonnull BaseElement defaultValue) {
        return overrides.getOrDefault(elementId, defaultValue);
    }

    /**
     * Get all values that have been set during the derivation process.
     * This includes the input values and the computed values.
     * The input values have priority over the computed values.
     *
     * @return The combined values.
     */
    @Nonnull
    public Map<String, Object> getCombinedValues() {
        var combinedValues = new HashMap<String, Object>();

        for (var entry : computedValues.entrySet()) {
            if (isVisible(entry.getKey()) && entry.getValue() != null) {
                combinedValues.put(entry.getKey(), entry.getValue());
            }
        }

        for (var entry : inputValues.entrySet()) {
            if (isVisible(entry.getKey()) && entry.getValue() != null) {
                combinedValues.put(entry.getKey(), entry.getValue());
            }
        }

        /*
        var combinedValues = new HashMap<>(computedValues);
        combinedValues.putAll(inputValues);
        // TODO: Clean empty values from the map
         */
        return combinedValues;
    }

    public Map<String, Boolean> getVisibilities() {
        return visibilities;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public Map<String, BaseElement> getOverrides() {
        return overrides;
    }
}
