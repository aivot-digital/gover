package de.aivot.prosuna.backend.elements.utils;

import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.elements.models.elements.LayoutElement;
import de.aivot.prosuna.backend.javascript.models.JavascriptCode;
import de.aivot.prosuna.backend.models.functions.conditions.ConditionSet;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperand;
import de.aivot.prosuna.backend.nocode.models.NoCodeProcessDataReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeReference;
import de.aivot.prosuna.backend.utils.StringUtils;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ElementReferenceUtils {
    @Nonnull
    public static Set<String> getReferencedIds(
            @Nullable JavascriptCode jsCode,
            @Nullable NoCodeOperand expression,
            @Nullable ConditionSet conditionSet
    ) {
        return getReferencedIds(jsCode, expression, conditionSet, Map.of());
    }

    @Nonnull
    public static Set<String> getReferencedIds(
            @Nullable JavascriptCode jsCode,
            @Nullable NoCodeOperand expression,
            @Nullable ConditionSet conditionSet,
            @Nonnull Map<String, ? extends Collection<String>> destinationKeyIndex
    ) {
        var referencedIds = new HashSet<String>();
        if (jsCode != null) {
            referencedIds.addAll(jsCode.getReferencedIds());
            for (var processDataPath : jsCode.getReferencedProcessDataPaths()) {
                addDestinationKeyReferences(referencedIds, destinationKeyIndex, processDataPath);
            }
        }
        if (expression != null) {
            addNoCodeReferences(referencedIds, destinationKeyIndex, expression);
        }
        if (conditionSet != null) {
            referencedIds.addAll(conditionSet.getReferencedIds());
        }
        return referencedIds;
    }

    @Nonnull
    public static Map<String, Set<String>> buildDestinationKeyIndex(@Nullable BaseElement rootElement) {
        var destinationKeyIndex = new HashMap<String, Set<String>>();
        collectDestinationKeyReferences(destinationKeyIndex, rootElement);
        return destinationKeyIndex;
    }

    private static void collectDestinationKeyReferences(
            @Nonnull Map<String, Set<String>> destinationKeyIndex,
            @Nullable BaseElement element
    ) {
        if (element == null) {
            return;
        }

        if (element instanceof BaseInputElement<?> inputElement) {
            var normalizedDestinationKey = normalizePath(inputElement.getDestinationKey());
            if (normalizedDestinationKey != null) {
                destinationKeyIndex
                        .computeIfAbsent(normalizedDestinationKey, ignored -> new HashSet<>())
                        .add(inputElement.getId());
            }
        }

        if (element instanceof LayoutElement<?> layoutElement) {
            for (var child : layoutElement.getChildren()) {
                collectDestinationKeyReferences(destinationKeyIndex, child);
            }
        }
    }

    private static void addNoCodeReferences(
            @Nonnull Set<String> referencedIds,
            @Nonnull Map<String, ? extends Collection<String>> destinationKeyIndex,
            @Nullable NoCodeOperand operand
    ) {
        if (operand == null) {
            return;
        }

        switch (operand) {
            case NoCodeReference reference -> {
                if (reference.getElementId() != null) {
                    referencedIds.add(reference.getElementId());
                }
            }
            case NoCodeProcessDataReference processDataReference ->
                    addDestinationKeyReferences(referencedIds, destinationKeyIndex, processDataReference.getPath());
            case NoCodeExpression noCodeExpression -> {
                var operands = noCodeExpression.getOperands();
                if (operands != null) {
                    for (var childOperand : operands) {
                        addNoCodeReferences(referencedIds, destinationKeyIndex, childOperand);
                    }
                }
            }
            default -> {
                // Do nothing
            }
        }
    }

    private static void addDestinationKeyReferences(
            @Nonnull Set<String> referencedIds,
            @Nonnull Map<String, ? extends Collection<String>> destinationKeyIndex,
            @Nullable String path
    ) {
        var normalizedPath = normalizePath(path);
        while (normalizedPath != null) {
            var matchingReferencedIds = destinationKeyIndex.get(normalizedPath);
            if (matchingReferencedIds != null) {
                referencedIds.addAll(matchingReferencedIds);
                return;
            }

            var lastSeparatorIndex = normalizedPath.lastIndexOf('.');
            normalizedPath = lastSeparatorIndex < 0 ? null : normalizedPath.substring(0, lastSeparatorIndex);
        }
    }

    @Nullable
    private static String normalizePath(@Nullable String path) {
        if (StringUtils.isNullOrEmpty(path)) {
            return null;
        }

        var normalizedPath = String.join(
                ".",
                Arrays.stream(path.split("\\."))
                        .map(String::trim)
                        .filter(StringUtils::isNotNullOrEmpty)
                        .toList()
        );

        return StringUtils.isNullOrEmpty(normalizedPath) ? null : normalizedPath;
    }
}
