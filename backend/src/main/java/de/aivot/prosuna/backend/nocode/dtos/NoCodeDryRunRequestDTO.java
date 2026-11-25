package de.aivot.prosuna.backend.nocode.dtos;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperand;
import jakarta.annotation.Nullable;

import java.util.Map;

/**
 * Request to evaluate a no-code expression with test data, without saving anything.
 *
 * @param expression   The expression to evaluate.
 * @param elementData  Test values and states of the referenced form elements.
 * @param processData  Test process data, referenced by process data references.
 * @param instanceData Test protected process data, referenced by instance data references.
 * @param nodeData     Test results of process elements, keyed by their data key.
 */
public record NoCodeDryRunRequestDTO(
        @Nullable NoCodeOperand expression,
        @Nullable DerivedRuntimeElementData elementData,
        @Nullable Map<String, Object> processData,
        @Nullable Map<String, Object> instanceData,
        @Nullable Map<String, Object> nodeData
) {
}
