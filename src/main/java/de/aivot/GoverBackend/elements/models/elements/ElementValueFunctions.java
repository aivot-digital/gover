package de.aivot.GoverBackend.elements.models.elements;

import de.aivot.GoverBackend.elements.utils.ElementReferenceUtils;
import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.nocode.models.NoCodeExpression;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

public class ElementValueFunctions implements Serializable {
    @Nullable
    private String requirements;
    @Nullable
    private NoCodeExpression expression;
    @Nullable
    private JavascriptCode javascriptCode;
    @Nullable
    private Collection<String> referencedIds;

    public void recalculateReferencedIds() {
        referencedIds = ElementReferenceUtils
                .getReferencedIds(
                        javascriptCode,
                        expression,
                        null // No ConditionSet for overrides
                );
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        ElementValueFunctions that = (ElementValueFunctions) o;
        return Objects.equals(requirements, that.requirements) && Objects.equals(expression, that.expression) && Objects.equals(javascriptCode, that.javascriptCode) && Objects.equals(referencedIds, that.referencedIds);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(requirements);
        result = 31 * result + Objects.hashCode(expression);
        result = 31 * result + Objects.hashCode(javascriptCode);
        result = 31 * result + Objects.hashCode(referencedIds);
        return result;
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public String getRequirements() {
        return requirements;
    }

    public ElementValueFunctions setRequirements(@Nullable String requirements) {
        this.requirements = requirements;
        return this;
    }

    @Nullable
    public NoCodeExpression getExpression() {
        return expression;
    }

    public ElementValueFunctions setExpression(@Nullable NoCodeExpression expression) {
        this.expression = expression;
        return this;
    }

    @Nullable
    public JavascriptCode getJavascriptCode() {
        return javascriptCode;
    }

    public ElementValueFunctions setJavascriptCode(@Nullable JavascriptCode javascriptCode) {
        this.javascriptCode = javascriptCode;
        return this;
    }

    @Nullable
    public Collection<String> getReferencedIds() {
        return referencedIds;
    }

    public ElementValueFunctions setReferencedIds(@Nullable Collection<String> referencedIds) {
        this.referencedIds = referencedIds;
        return this;
    }

    // endregion
}
