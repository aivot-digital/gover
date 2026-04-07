package de.aivot.GoverBackend.elements.models.elements;

import de.aivot.GoverBackend.elements.enums.OverrideFunctionType;
import de.aivot.GoverBackend.elements.utils.ElementReferenceUtils;
import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.nocode.models.NoCodeOperand;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public class ElementOverrideFunctions implements Serializable {
    @Nullable
    OverrideFunctionType type;
    @Nullable
    private String requirements;
    @Nullable
    private Map<String, NoCodeOperand> fieldNoCodeMap;
    @Nullable
    private JavascriptCode javascriptCode;
    @Nullable
    private Collection<String> referencedIds;

    public void recalculateReferencedIds() {
        referencedIds = ElementReferenceUtils
                .getReferencedIds(
                        javascriptCode,
                        null,
                        null // No ConditionSet for overrides
                );

        if (fieldNoCodeMap != null) {
            for (var expression : fieldNoCodeMap.values()) {
                if (expression == null) {
                    continue;
                }
                referencedIds.addAll(ElementReferenceUtils
                        .getReferencedIds(
                                null,
                                expression,
                                null
                        ));
            }
        }
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        ElementOverrideFunctions that = (ElementOverrideFunctions) o;
        return type == that.type && Objects.equals(requirements, that.requirements) && Objects.equals(fieldNoCodeMap, that.fieldNoCodeMap) && Objects.equals(javascriptCode, that.javascriptCode) && Objects.equals(referencedIds, that.referencedIds);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(type);
        result = 31 * result + Objects.hashCode(requirements);
        result = 31 * result + Objects.hashCode(fieldNoCodeMap);
        result = 31 * result + Objects.hashCode(javascriptCode);
        result = 31 * result + Objects.hashCode(referencedIds);
        return result;
    }


    // endregion

    // region Getters & Setters


    @Nullable
    public OverrideFunctionType getType() {
        return type;
    }

    public ElementOverrideFunctions setType(@Nullable OverrideFunctionType type) {
        this.type = type;
        return this;
    }

    @Nullable
    public String getRequirements() {
        return requirements;
    }

    public ElementOverrideFunctions setRequirements(@Nullable String requirements) {
        this.requirements = requirements;
        return this;
    }

    @Nullable
    public Map<String, NoCodeOperand> getFieldNoCodeMap() {
        return fieldNoCodeMap;
    }

    public ElementOverrideFunctions setFieldNoCodeMap(@Nullable Map<String, NoCodeOperand> fieldNoCodeMap) {
        this.fieldNoCodeMap = fieldNoCodeMap;
        if (this.type == null && fieldNoCodeMap != null) {
            this.type = OverrideFunctionType.NoCode;
        }
        return this;
    }

    @Nullable
    public JavascriptCode getJavascriptCode() {
        return javascriptCode;
    }

    public ElementOverrideFunctions setJavascriptCode(@Nullable JavascriptCode javascriptCode) {
        this.javascriptCode = javascriptCode;
        if (this.type == null && javascriptCode != null) {
            this.type = OverrideFunctionType.Javascript;
        }
        return this;
    }

    @Nullable
    public Collection<String> getReferencedIds() {
        return referencedIds;
    }

    public ElementOverrideFunctions setReferencedIds(@Nullable Collection<String> referencedIds) {
        this.referencedIds = referencedIds;
        return this;
    }

    // endregion
}
