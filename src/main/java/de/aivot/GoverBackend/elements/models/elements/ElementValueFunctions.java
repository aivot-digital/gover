package de.aivot.GoverBackend.elements.models.elements;

import de.aivot.GoverBackend.elements.enums.ValueFunctionType;
import de.aivot.GoverBackend.elements.utils.ElementReferenceUtils;
import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.nocode.models.NoCodeOperand;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

public class ElementValueFunctions implements Serializable {
    @Nullable
    ValueFunctionType type;
    @Nullable
    private String requirements;
    @Nullable
    private NoCodeOperand noCode;
    @Nullable
    private JavascriptCode javascriptCode;
    @Nullable
    private Collection<String> referencedIds;

    public void recalculateReferencedIds() {
        referencedIds = ElementReferenceUtils
                .getReferencedIds(
                        javascriptCode,
                        noCode,
                        null // No ConditionSet for overrides
                );
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        ElementValueFunctions that = (ElementValueFunctions) o;
        return type == that.type && Objects.equals(requirements, that.requirements) && Objects.equals(noCode, that.noCode) && Objects.equals(javascriptCode, that.javascriptCode) && Objects.equals(referencedIds, that.referencedIds);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(type);
        result = 31 * result + Objects.hashCode(requirements);
        result = 31 * result + Objects.hashCode(noCode);
        result = 31 * result + Objects.hashCode(javascriptCode);
        result = 31 * result + Objects.hashCode(referencedIds);
        return result;
    }


    // endregion

    // region Getters & Setters


    @Nullable
    public ValueFunctionType getType() {
        return type;
    }

    public ElementValueFunctions setType(@Nullable ValueFunctionType type) {
        this.type = type;
        return this;
    }

    @Nullable
    public String getRequirements() {
        return requirements;
    }

    public ElementValueFunctions setRequirements(@Nullable String requirements) {
        this.requirements = requirements;
        return this;
    }

    @Nullable
    public NoCodeOperand getNoCode() {
        return noCode;
    }

    public ElementValueFunctions setNoCode(@Nullable NoCodeOperand noCode) {
        this.noCode = noCode;
        if (this.type == null && noCode != null) {
            this.type = ValueFunctionType.NoCode;
        }
        return this;
    }

    @Nullable
    public JavascriptCode getJavascriptCode() {
        return javascriptCode;
    }

    public ElementValueFunctions setJavascriptCode(@Nullable JavascriptCode javascriptCode) {
        this.javascriptCode = javascriptCode;
        if (this.type == null && javascriptCode != null) {
            this.type = ValueFunctionType.Javascript;
        }
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
