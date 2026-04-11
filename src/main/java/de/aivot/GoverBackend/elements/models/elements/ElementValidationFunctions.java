package de.aivot.GoverBackend.elements.models.elements;

import de.aivot.GoverBackend.elements.enums.ValidationFunctionType;
import de.aivot.GoverBackend.elements.utils.ElementReferenceUtils;
import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.models.functions.conditions.ConditionSet;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ElementValidationFunctions implements Serializable {
    @Nullable
    private ValidationFunctionType type;
    @Nullable
    private String requirements;
    @Nullable
    private ConditionSet conditionSet;
    @Nullable
    private List<ValidationNoCodeWrapper> noCodeList;
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

        if (noCodeList != null) {
            for (var validationExpression : noCodeList) {
                if (validationExpression.getNoCode() == null) {
                    continue;
                }

                referencedIds.addAll(ElementReferenceUtils
                        .getReferencedIds(
                                null,
                                validationExpression.getNoCode(),
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
        return Objects.equals(type, that.type) && Objects.equals(requirements, that.requirements) && Objects.equals(conditionSet, that.conditionSet) && Objects.equals(noCodeList, that.noCodeList) && Objects.equals(javascriptCode, that.javascriptCode) && Objects.equals(referencedIds, that.referencedIds);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(type);
        result = 31 * result + Objects.hashCode(requirements);
        result = 31 * result + Objects.hashCode(conditionSet);
        result = 31 * result + Objects.hashCode(noCodeList);
        result = 31 * result + Objects.hashCode(javascriptCode);
        result = 31 * result + Objects.hashCode(referencedIds);
        return result;
    }


    // endregion

    // region Getters & Setters

    @Nullable
    public ValidationFunctionType getType() {
        return type;
    }

    public ElementValidationFunctions setType(@Nullable ValidationFunctionType type) {
        this.type = type;
        return this;
    }

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
        if (this.type == null && conditionSet != null) {
            this.type = ValidationFunctionType.ConditionSet;
        }
        return this;
    }

    @Nullable
    public List<ValidationNoCodeWrapper> getNoCodeList() {
        return noCodeList;
    }

    public ElementValidationFunctions setNoCodeList(@Nullable List<ValidationNoCodeWrapper> noCodeList) {
        this.noCodeList = noCodeList;
        if (this.type == null && noCodeList != null) {
            this.type = ValidationFunctionType.NoCode;
        }
        return this;
    }

    @Nullable
    public JavascriptCode getJavascriptCode() {
        return javascriptCode;
    }

    public ElementValidationFunctions setJavascriptCode(@Nullable JavascriptCode javascriptCode) {
        this.javascriptCode = javascriptCode;
        if (this.type == null && javascriptCode != null) {
            this.type = ValidationFunctionType.Javascript;
        }
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
