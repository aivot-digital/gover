package de.aivot.GoverBackend.process.models;

import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;

/**
 *
 * @param node The node the problems were computed for.
 * @param problems The human-readable list of all problems.
 * @param commonErrors A map of errors concerning common node configuration fields which are not part of the use case specific configuration layout of a node.
 * @param derivedRuntimeElementData The derived runtime data of the configuration layout of the node which contains eventual errors in the computed states.
 */
public record ProcessNodeProblems(
        @Nonnull ProcessNodeEntity node,
        @Nonnull List<String> problems,
        @Nonnull Map<String, String> commonErrors,
        @Nonnull DerivedRuntimeElementData derivedRuntimeElementData
) {
    public static final String COMMON_ERROR_KEY_DATA_KEY = "dataKey";
}
