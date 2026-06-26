package de.aivot.gover.backend.process.models;

import de.aivot.gover.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
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
        @Nonnull Map<String, List<String>> commonErrors,
        @Nonnull DerivedRuntimeElementData derivedRuntimeElementData
) {
    public static final String COMMON_ERROR_KEY_DATA_KEY = "dataKey";

    public boolean hasAnyProblems() {
        return !problems.isEmpty() || !commonErrors.isEmpty() || derivedRuntimeElementData.hasAnyError();
    }
}
