package de.aivot.GoverBackend.elements.utils;

import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.models.functions.Function;
import de.aivot.GoverBackend.nocode.models.NoCodeExpression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public class ElementReferenceUtils {
    @Nonnull
    public static Set<String> getReferencedIds(
            @Nullable JavascriptCode jsCode,
            @Nullable NoCodeExpression expression,
            @Nullable Function func
    ) {
        var referencedIds = new HashSet<String>();
        if (jsCode != null) {
            referencedIds.addAll(jsCode.getReferencedIds());
        }
        if (expression != null) {
            referencedIds.addAll(expression.getReferencedIds());
        }
        if (func != null) {
            referencedIds.addAll(func.getReferencedIds());
        }
        return referencedIds;
    }
}
