package de.aivot.GoverBackend.elements.utils;

import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.models.functions.conditions.ConditionSet;
import de.aivot.GoverBackend.nocode.models.NoCodeExpression;
import de.aivot.GoverBackend.nocode.models.NoCodeOperand;
import de.aivot.GoverBackend.nocode.models.NoCodeReference;
import de.aivot.GoverBackend.nocode.models.NoCodeStaticValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public class ElementReferenceUtils {
    @Nonnull
    public static Set<String> getReferencedIds(
            @Nullable JavascriptCode jsCode,
            @Nullable NoCodeOperand expression,
            @Nullable ConditionSet conditionSet
    ) {
        var referencedIds = new HashSet<String>();
        if (jsCode != null) {
            referencedIds.addAll(jsCode.getReferencedIds());
        }
        if (expression != null) {
            switch (expression) {
                case NoCodeReference reference -> referencedIds.add(reference.getElementId());
                case NoCodeExpression noCodeExpression -> referencedIds.addAll(noCodeExpression.getReferencedIds());
                default -> {
                    // Do nothing
                }
            }
        }
        if (conditionSet != null) {
            referencedIds.addAll(conditionSet.getReferencedIds());
        }
        return referencedIds;
    }
}
