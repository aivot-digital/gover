package de.aivot.prosuna.backend.process.models.processContext;

import de.aivot.prosuna.backend.elements.models.ComputedElementState;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeConfigurationValidationPhase;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * Supplies node-specific validation with both the mapped configuration and the derivation state behind it.
 * Dynamic values deliberately map to {@code null} during authoring, so validators must use {@link #isDeferred(String)}
 * before applying checks that require the concrete value.
 */
public record ProcessNodeConfigurationValidationContext<NodeConfig>(
        @Nonnull ProcessNodeEntity thisNode,
        @Nonnull NodeConfig configuration,
        @Nonnull DerivedRuntimeElementData derivedRuntimeElementData,
        @Nonnull ProcessNodeConfigurationValidationPhase phase
) {
    public ProcessNodeConfigurationValidationContext {
        Objects.requireNonNull(thisNode);
        Objects.requireNonNull(configuration);
        Objects.requireNonNull(derivedRuntimeElementData);
        Objects.requireNonNull(phase);
    }

    public boolean isAuthoring() {
        return phase == ProcessNodeConfigurationValidationPhase.Authoring;
    }

    public boolean isRuntime() {
        return phase == ProcessNodeConfigurationValidationPhase.Runtime;
    }

    /**
     * Returns whether a top-level field contains a valid dynamic value that is awaiting runtime process data.
     */
    public boolean isDeferred(@Nonnull String elementId) {
        if (!isAuthoring()) {
            return false;
        }

        return isDeferred(derivedRuntimeElementData.getElementStates().get(elementId));
    }

    /**
     * Returns the deferred state of a field in one row of a replicating container. Row indices match the mapped
     * configuration list because derivation preserves authored row order.
     */
    public boolean isDeferred(@Nonnull String containerElementId,
                              int rowIndex,
                              @Nonnull String childElementId) {
        if (!isAuthoring()) {
            return false;
        }

        var containerState = derivedRuntimeElementData.getElementStates().get(containerElementId);
        if (isDeferred(containerState)) {
            return true;
        }
        if (containerState == null ||
                containerState.getSubStates() == null ||
                rowIndex < 0 ||
                rowIndex >= containerState.getSubStates().size()) {
            return false;
        }

        var rowState = containerState.getSubStates().get(rowIndex);
        return isDeferred(rowState.getStates().get(childElementId));
    }

    private static boolean isDeferred(@Nullable ComputedElementState state) {
        return state != null && state.isInputValueDeferred();
    }
}
