package de.aivot.GoverBackend.elements.models.elements;

import de.aivot.GoverBackend.elements.utils.ElementReferenceUtils;
import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.models.functions.conditions.ConditionSet;
import de.aivot.GoverBackend.models.lib.ValidationExpressionWrapper;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ElementValidationFunctions {
    @Nullable
    private String requirements;
    @Nullable
    private ConditionSet conditionSet;
    @Nullable
    private List<ValidationExpressionWrapper> validationExpressions;
    @Nullable
    private JavascriptCode javascriptCode;
    @Nullable
    private Collection<String> referencedIds;

    public void recalculateReferencedIds() {
        referencedIds = ElementReferenceUtils
                .getReferencedIds(
                        javascriptCode,
                        null,
                        conditionSet
                );

        if (validationExpressions != null) {
            for (var validationExpression : validationExpressions) {
                if (validationExpression.getExpression() == null) {
                    continue;
                }

                referencedIds.addAll(ElementReferenceUtils
                        .getReferencedIds(
                                null,
                                validationExpression.getExpression(),
                                null
                        ));
            }
        }
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        ElementValidationFunctions that = (ElementValidationFunctions) o;
        return Objects.equals(requirements, that.requirements) && Objects.equals(conditionSet, that.conditionSet) && Objects.equals(validationExpressions, that.validationExpressions) && Objects.equals(javascriptCode, that.javascriptCode) && Objects.equals(referencedIds, that.referencedIds);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(requirements);
        result = 31 * result + Objects.hashCode(conditionSet);
        result = 31 * result + Objects.hashCode(validationExpressions);
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

    public ElementValidationFunctions setRequirements(@Nullable String requirements) {
        this.requirements = requirements;
        return this;
    }

    @Nullable
    public ConditionSet getConditionSet() {
        return conditionSet;
    }

    public ElementValidationFunctions setConditionSet(@Nullable ConditionSet conditionSet) {
        this.conditionSet = conditionSet;
        return this;
    }

    @Nullable
    public List<ValidationExpressionWrapper> getValidationExpressions() {
        return validationExpressions;
    }

    public ElementValidationFunctions setValidationExpressions(@Nullable List<ValidationExpressionWrapper> validationExpressions) {
        this.validationExpressions = validationExpressions;
        return this;
    }

    @Nullable
    public JavascriptCode getJavascriptCode() {
        return javascriptCode;
    }

    public ElementValidationFunctions setJavascriptCode(@Nullable JavascriptCode javascriptCode) {
        this.javascriptCode = javascriptCode;
        return this;
    }

    @Nullable
    public Collection<String> getReferencedIds() {
        return referencedIds;
    }

    public ElementValidationFunctions setReferencedIds(@Nullable Collection<String> referencedIds) {
        this.referencedIds = referencedIds;
        return this;
    }

    // endregion
}
