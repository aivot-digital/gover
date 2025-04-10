package de.aivot.GoverBackend.elements.models;

import de.aivot.GoverBackend.javascript.services.JavascriptEngine;
import de.aivot.GoverBackend.nocode.services.NoCodeEvaluationService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.script.ScriptEngine;
import java.util.*;

/**
 * This is the context for any element derivation.
 * It should be used, to store information during the derivation process.
 * This information is then used to render the elements the derivation was made for.
 */
public abstract class BaseElementDerivationContext implements AutoCloseable {
    public static final String ID_JS_CONTEXT_OBJECT_NAME = "id";
    public static final String INPUT_VALUES_JS_CONTEXT_OBJECT_NAME = "inputValues";
    public static final String COMPUTED_VALUES_JS_CONTEXT_OBJECT_NAME = "computedValues";
    public static final String VISIBILITIES_JS_CONTEXT_OBJECT_NAME = "visibilities";
    public static final String ERRORS_JS_CONTEXT_OBJECT_NAME = "errors";
    public static final String OVERRIDES_JS_CONTEXT_OBJECT_NAME = "overrides";
    public static final String ELEMENT_JS_CONTEXT_OBJECT_NAME = "element";

    protected final JavascriptEngine javascriptEngine;
    protected final ScriptEngine legacyJavascriptEngine;
    protected final NoCodeEvaluationService noCodeEvaluationService;

    protected final BaseElement rootElement;

    protected final ElementDerivationData elementDerivationData;

    public BaseElementDerivationContext(
            @Nonnull JavascriptEngine javascriptEngine,
            @Nonnull ScriptEngine legacyJavascriptEngine,
            @Nonnull NoCodeEvaluationService noCodeEvaluationService,
            @Nonnull BaseElement rootElement,
            @Nonnull Map<String, Object> inputValues
    ) {
        this.javascriptEngine = javascriptEngine;
        this.legacyJavascriptEngine = legacyJavascriptEngine;
        this.noCodeEvaluationService = noCodeEvaluationService;
        this.rootElement = rootElement;
        this.elementDerivationData = new ElementDerivationData(inputValues);
    }

    /**
     * Set the visibility of a field based on the id of the element.
     * Only invisibilities are explicitly set, because all elements are visible by default.
     *
     * @param elementId The id of the element.
     * @param value     The visibility of the element. If the value is null or true, the visibility will be set to true.
     * @deprecated Use {@link ElementDerivationData#setVisibility(String, Boolean)} instead.
     */
    @Deprecated
    public void setVisibility(@Nonnull String elementId, @Nullable Boolean value) {
        elementDerivationData.setVisibility(elementId, value);
    }

    /**
     * Check if an element is visible based on the id of the element.
     * If the visibility is not set, the method will return true because all elements are visible by default.
     *
     * @param elementId The id of the element.
     * @return The visibility of the element.
     * @deprecated Use {@link ElementDerivationData#isVisible(String)} instead.
     */
    @Nonnull
    @Deprecated
    public Boolean isVisible(@Nonnull String elementId) {
        return elementDerivationData.isVisible(elementId);
    }

    /**
     * Check if an element is invisible based on the id of the element.
     * If the visibility is not set, the method will return false because all elements are visible by default.
     *
     * @param elementId The id of the element.
     * @return The invisibility of the element.
     * @deprecated Use {@link ElementDerivationData#isInvisible(String)} instead.
     */
    @Nonnull
    @Deprecated
    public Boolean isInvisible(@Nonnull String elementId) {
        return elementDerivationData.isInvisible(elementId);
    }

    /**
     * Set the value of a field based on the id of the element.
     *
     * @param elementId The id of the element.
     * @param value     The value of the element.
     * @deprecated Use {@link ElementDerivationData#setValue(String, Object)} instead.
     */
    @Deprecated
    public void setValue(@Nonnull String elementId, @Nullable Object value) {
        elementDerivationData.setValue(elementId, value);
    }

    /**
     * Check if a field has a value based on the id of the element.
     * If the element is invisible, the method will return false because invisible elements have no value.
     *
     * @param elementId The id of the element.
     * @return Returns true if the element has a value, otherwise false.
     * @deprecated Use {@link ElementDerivationData#hasValue(String)} instead.
     */
    @Deprecated
    public boolean hasValue(@Nonnull String elementId) {
        return elementDerivationData.hasValue(elementId);
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
     * @deprecated Use {@link ElementDerivationData#getValue(String)} instead.
     */
    @Nonnull
    @Deprecated
    public Optional<Object> getValue(@Nonnull String elementId) {
        return elementDerivationData.getValue(elementId);
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
     * @deprecated Use {@link ElementDerivationData#getValue(String, Class)} instead.
     */
    @Nonnull
    @Deprecated
    public <T> Optional<T> getValue(@Nonnull String elementId, @Nonnull Class<T> desiredClass) {
        return elementDerivationData.getValue(elementId, desiredClass);
    }

    /**
     * Set the error of a field based on the id of the element.
     *
     * @param elementId The id of the element.
     * @param error     The error of the element.
     * @deprecated Use {@link ElementDerivationData#setError(String, String)} instead.
     */
    @Deprecated
    public void setError(@Nonnull String elementId, @Nullable String error) {
        elementDerivationData.setError(elementId, error);
    }

    /**
     * Get the error of a field based on the id of the element.
     * If the error is not set, the method will return an empty optional.
     *
     * @param elementId The id of the element.
     * @return The error of the element.
     * @deprecated Use {@link ElementDerivationData#getError(String)} instead.
     */
    @Nonnull
    @Deprecated
    public Optional<String> getError(@Nonnull String elementId) {
        return elementDerivationData.getError(elementId);
    }

    /**
     * Check if the derivation has produced any errors.
     *
     * @return Returns true if there are any errors, otherwise false.
     * @deprecated Use {@link ElementDerivationData#hasErrors()} instead.
     */
    @Nonnull
    @Deprecated
    public Boolean hasErrors() {
        return elementDerivationData.hasErrors();
    }

    /**
     * Set the element override of a field based on the id of the element.
     *
     * @param elementId The id of the element.
     * @param override  The element override.
     * @deprecated Use {@link ElementDerivationData#setOverride(String, BaseElement)} instead.
     */
    @Deprecated
    public void setOverride(@Nonnull String elementId, @Nullable BaseElement override) {
        elementDerivationData.setOverride(elementId, override);
    }

    /**
     * Get the element override of a field based on the id of the element.
     * If the element override is not set, the method will return an empty optional.
     *
     * @param elementId The id of the element.
     * @return The element override.
     * @deprecated Use {@link ElementDerivationData#getOverride(String)} instead.
     */
    @Nonnull
    @Deprecated
    public Optional<BaseElement> getOverride(@Nonnull String elementId) {
        return elementDerivationData.getOverride(elementId);
    }

    /**
     * Get the element override of a field based on the id of the element.
     * If the element override is not set, the method will return the default value.
     *
     * @param elementId    The id of the element.
     * @param defaultValue The default value.
     * @return The element override.
     * @deprecated Use {@link ElementDerivationData#getOverride(String, BaseElement)} instead.
     */
    @Nonnull
    @Deprecated
    public BaseElement getOverride(@Nonnull String elementId, @Nonnull BaseElement defaultValue) {
        return elementDerivationData.getOverride(elementId, defaultValue);
    }

    /**
     * Get all values that have been set during the derivation process.
     * This includes the input values and the computed values.
     * The input values have priority over the computed values.
     *
     * @return The combined values.
     * @deprecated Use {@link ElementDerivationData#getCombinedValues()} instead.
     */
    @Nonnull
    @Deprecated
    public Map<String, Object> getCombinedValues() {
        return elementDerivationData.getCombinedValues();
    }

    /**
     * Get the Javascript engine for executing Javascript code.
     *
     * @return The Javascript engine.
     */
    @Nonnull
    public JavascriptEngine getJavascriptEngine() {
        return javascriptEngine;
    }

    /**
     * Get the Javascript context object that can be injected into the Javascript engine.
     *
     * @return The Javascript context object.
     */
    @Nonnull
    public Map<String, Object> getJavascriptContextObject(String resolvedId, BaseElement currentElement) {
        var map = new HashMap<String, Object>();

        map.put(ID_JS_CONTEXT_OBJECT_NAME, resolvedId);

        map.put(INPUT_VALUES_JS_CONTEXT_OBJECT_NAME, elementDerivationData.inputValues);
        map.put(COMPUTED_VALUES_JS_CONTEXT_OBJECT_NAME, elementDerivationData.computedValues);
        map.put(VISIBILITIES_JS_CONTEXT_OBJECT_NAME, elementDerivationData.visibilities);
        map.put(ERRORS_JS_CONTEXT_OBJECT_NAME, elementDerivationData.errors);
        map.put(OVERRIDES_JS_CONTEXT_OBJECT_NAME, elementDerivationData.overrides);
        map.put(ELEMENT_JS_CONTEXT_OBJECT_NAME, currentElement);

        return map;
    }

    /**
     * Get the legacy Javascript engine for executing legacy Javascript code.
     * This method is deprecated and should not be used anymore.
     *
     * @return The legacy Javascript engine.
     * @deprecated Use {@link #getJavascriptEngine()} instead.
     */
    @Nonnull
    public ScriptEngine getLegacyJavascriptEngine() {
        return legacyJavascriptEngine;
    }

    /**
     * Get the NoCodeEvaluationService for evaluating no code expressions.
     * This service is used to evaluate expressions that do not contain any code.
     *
     * @return The NoCodeEvaluationService.
     */
    @Nonnull
    public NoCodeEvaluationService getNoCodeEvaluationService() {
        return noCodeEvaluationService;
    }

    /**
     * Get the root element of the derivation context.
     * The root element is the topmost element this context was created for.
     *
     * @return The root element.
     */
    @Nonnull
    public BaseElement getRootElement() {
        return rootElement;
    }

    @Nonnull
    public ElementDerivationData getElementDerivationData() {
        return elementDerivationData;
    }

    /**
     * Close the context and release all resources.
     * This includes closing the Javascript engine.
     *
     * @throws Exception If an error occurs during the closing process.
     */
    @Override
    public void close() throws Exception {
        javascriptEngine.close();
    }
}
