package de.aivot.GoverBackend.elements.models.elements;

import de.aivot.GoverBackend.elements.enums.VisibilityFunctionType;
import de.aivot.GoverBackend.elements.utils.ElementReferenceUtils;
import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.models.functions.conditions.ConditionSet;
import de.aivot.GoverBackend.nocode.models.NoCodeOperand;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

public class ElementVisibilityFunctions implements Serializable {
    @Nullable
    VisibilityFunctionType type;
    @Nullable
    private String requirements;
    @Nullable
    private ConditionSet conditionSet;
    @Nullable
    private NoCodeOperand noCode;
    @Nullable
    private JavascriptCode javascriptCode;
    @Nullable
    private Collection<String> referencedIds;

    public ElementVisibilityFunctions recalculateReferencedIds() {
        referencedIds = ElementReferenceUtils
                .getReferencedIds(
                        javascriptCode,
                        noCode,
                        conditionSet
                );
        return this;
    }

    public static ElementVisibilityFunctions of(NoCodeOperand noCode) {
        return new ElementVisibilityFunctions()
                .setType(VisibilityFunctionType.NoCode)
                .setNoCode(noCode);
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        ElementVisibilityFunctions that = (ElementVisibilityFunctions) o;
        return type == that.type && Objects.equals(requirements, that.requirements) && Objects.equals(conditionSet, that.conditionSet) && Objects.equals(noCode, that.noCode) && Objects.equals(javascriptCode, that.javascriptCode) && Objects.equals(referencedIds, that.referencedIds);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(type);
        result = 31 * result + Objects.hashCode(requirements);
        result = 31 * result + Objects.hashCode(conditionSet);
        result = 31 * result + Objects.hashCode(noCode);
        result = 31 * result + Objects.hashCode(javascriptCode);
        result = 31 * result + Objects.hashCode(referencedIds);
        return result;
    }


    // endregion

    // region Getters & Setters


    @Nullable
    public VisibilityFunctionType getType() {
        return type;
    }

    public ElementVisibilityFunctions setType(@Nullable VisibilityFunctionType type) {
        this.type = type;
        return this;
    }

    @Nullable
    public String getRequirements() {
        return requirements;
    }

    public ElementVisibilityFunctions setRequirements(@Nullable String requirements) {
        this.requirements = requirements;
        return this;
    }

    @Nullable
    public ConditionSet getConditionSet() {
        return conditionSet;
    }

    public ElementVisibilityFunctions setConditionSet(@Nullable ConditionSet conditionSet) {
        this.conditionSet = conditionSet;
        return this;
    }

    @Nullable
    public NoCodeOperand getNoCode() {
        return noCode;
    }

    public ElementVisibilityFunctions setNoCode(@Nullable NoCodeOperand noCode) {
        this.noCode = noCode;
        return this;
    }

    @Nullable
    public JavascriptCode getJavascriptCode() {
        return javascriptCode;
    }

    public ElementVisibilityFunctions setJavascriptCode(@Nullable JavascriptCode javascriptCode) {
        this.javascriptCode = javascriptCode;
        return this;
    }

    @Nullable
    public Collection<String> getReferencedIds() {
        return referencedIds;
    }

    public ElementVisibilityFunctions setReferencedIds(@Nullable Collection<String> referencedIds) {
        this.referencedIds = referencedIds;
        return this;
    }

    // endregion
}
