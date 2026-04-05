package de.aivot.GoverBackend.submission.services;

import de.aivot.GoverBackend.elements.models.EffectiveElementValues;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.LayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Builds the outbound destination payload from authored element values and the element tree.
 * <p>
 * The form model stores user input by element id, while downstream integrations usually expect a
 * domain-shaped payload addressed by destination keys. This service is the bridge between those
 * two representations: it walks the configured elements, looks up their effective values and
 * materializes the nested object/array structure described by each element's
 * {@code destinationKey}.
 * <p>
 * The implementation is intentionally structure-driven instead of value-driven. It only exports
 * values for elements that explicitly declare a destination key and it derives nesting from the
 * form definition itself. This keeps the payload stable and predictable even when unrelated values
 * exist in the effective value map.
 * <p>
 * In addition to regular dotted object paths, destination keys may address arrays. Numeric path
 * segments such as {@code members.0.first_name} target a concrete array slot, while {@code *}
 * behaves as a contextual wildcard:
 * <ul>
 *     <li>inside a replicating container it resolves to the current row index so repeated form
 *     rows can write into the matching payload item,</li>
 *     <li>outside a replicating container it is treated as a broadcast over the existing target
 *     array so a single value can be copied to all items.</li>
 * </ul>
 * This dual behavior allows form authors to mix row-local mappings with cross-cutting mappings
 * without needing separate transformation passes.
 */
@Service
public class ElementDataTransformService {
    private static final Object PAYLOAD_VALUE_NOT_FOUND = new Object();

    /**
     * Creates a payload map for the given form tree.
     * <p>
     * The returned map is suitable for downstream submission targets that address data by
     * destination keys rather than internal element ids.
     *
     * @param rootElement the root of the form tree that defines which values should be exported
     *                    and where they should be written
     * @param effectiveValues the resolved element values keyed by element id
     * @return a newly built payload containing only data referenced by destination keys
     */
    @Nonnull
    public Map<String, Object> buildPayload(@Nullable BaseElement rootElement,
                                            @Nonnull Map<String, Object> effectiveValues) {
        var payload = new LinkedHashMap<String, Object>();
        mergeDestinationKeyPayload(rootElement, effectiveValues, payload, List.of());
        return payload;
    }

    /**
     * Creates effective element values from a destination payload and the form tree.
     * <p>
     * This is the inverse view of {@link #buildPayload(BaseElement, Map)}. The payload alone is not
     * enough to reconstruct form values because only the element tree defines which destination
     * keys are relevant and how replicated rows should be grouped back into per-element values.
     *
     * @param rootElement the root of the form tree that defines which destination keys should be
     *                    read from the payload
     * @param payload the destination-shaped payload keyed by external data paths
     * @return effective values keyed by element id
     */
    @Nonnull
    public EffectiveElementValues buildEffectiveValues(@Nullable BaseElement rootElement,
                                                       @Nonnull Map<String, Object> payload) {
        var effectiveValues = new EffectiveElementValues();
        mergeEffectiveValues(rootElement, payload, effectiveValues, List.of());
        return effectiveValues;
    }

    /**
     * Traverses the element tree and delegates payload generation based on element type.
     * <p>
     * Replication indices are carried alongside the traversal so nested writes can resolve
     * wildcard array segments relative to the current replicating-container row. Passing that
     * context through the traversal avoids coupling path resolution to a specific element type and
     * keeps wildcard handling consistent for all descendants.
     */
    private void mergeDestinationKeyPayload(@Nullable BaseElement element,
                                            @Nonnull Map<String, Object> effectiveValues,
                                            @Nonnull Map<String, Object> payload,
                                            @Nonnull List<Integer> replicationIndices) {
        if (element == null) {
            return;
        }

        if (element instanceof ReplicatingContainerLayoutElement replicatingContainer) {
            mergeReplicatingContainerPayload(replicatingContainer, effectiveValues, payload, replicationIndices);
            return;
        }

        if (element instanceof BaseInputElement<?> inputElement) {
            mergeInputPayload(inputElement, effectiveValues, payload, replicationIndices);
        }

        if (element instanceof LayoutElement<?> layoutElement) {
            for (var child : layoutElement.getChildren()) {
                mergeDestinationKeyPayload(child, effectiveValues, payload, replicationIndices);
            }
        }
    }

    /**
     * Traverses the element tree and reconstructs effective element values from the payload.
     * <p>
     * The traversal mirrors the forward mapping so the same structural rules are used in both
     * directions. This keeps reverse mapping deterministic even when different elements point into
     * the same payload subtree.
     */
    private void mergeEffectiveValues(@Nullable BaseElement element,
                                      @Nonnull Map<String, Object> payload,
                                      @Nonnull EffectiveElementValues effectiveValues,
                                      @Nonnull List<Integer> replicationIndices) {
        if (element == null) {
            return;
        }

        if (element instanceof ReplicatingContainerLayoutElement replicatingContainer) {
            mergeReplicatingContainerEffectiveValues(replicatingContainer, payload, effectiveValues, replicationIndices);
            return;
        }

        if (element instanceof BaseInputElement<?> inputElement) {
            mergeInputEffectiveValue(inputElement, payload, effectiveValues, replicationIndices);
        }

        if (element instanceof LayoutElement<?> layoutElement) {
            for (var child : layoutElement.getChildren()) {
                mergeEffectiveValues(child, payload, effectiveValues, replicationIndices);
            }
        }
    }

    /**
     * Maps the value of a replicating container into the destination payload.
     * <p>
     * Replicating containers are special because they contribute both data and positional context.
     * Each item in the source list may become a payload object on its own, but the item index is
     * also relevant for descendant destination keys that use {@code *}. By propagating the current
     * item index to child traversal, the service can support destination keys such as
     * {@code members.*.first_name} without requiring child elements to know anything about their
     * surrounding container.
     * <p>
     * When the container itself has no destination key, its children still participate in payload
     * generation. This allows a replicating UI structure to write directly into an existing payload
     * array defined elsewhere instead of forcing a one-to-one mapping between UI containers and
     * payload containers.
     */
    private void mergeReplicatingContainerPayload(@Nonnull ReplicatingContainerLayoutElement replicatingContainer,
                                                  @Nonnull Map<String, Object> effectiveValues,
                                                  @Nonnull Map<String, Object> payload,
                                                  @Nonnull List<Integer> replicationIndices) {
        if (!effectiveValues.containsKey(replicatingContainer.getId())) {
            return;
        }

        var rawValue = effectiveValues.get(replicatingContainer.getId());
        if (rawValue == null) {
            writePayloadValue(payload, replicatingContainer.getDestinationKey(), null, replicationIndices);
            return;
        }

        if (!(rawValue instanceof List<?> rawItems)) {
            writePayloadValue(payload, replicatingContainer.getDestinationKey(), rawValue, replicationIndices);
            return;
        }

        var mappedItems = new LinkedList<Object>();
        for (var itemIndex = 0; itemIndex < rawItems.size(); itemIndex++) {
            var rawItem = rawItems.get(itemIndex);
            if (rawItem instanceof Map<?, ?> rawItemMap) {
                var itemEffectiveValues = toStringObjectMap(rawItemMap);
                var itemReplicationIndices = appendReplicationIndex(replicationIndices, itemIndex);

                if (StringUtils.isNullOrEmpty(replicatingContainer.getDestinationKey())) {
                    for (var child : replicatingContainer.getChildren()) {
                        mergeDestinationKeyPayload(child, itemEffectiveValues, payload, itemReplicationIndices);
                    }

                    continue;
                }

                var itemPayload = new LinkedHashMap<String, Object>();
                for (var child : replicatingContainer.getChildren()) {
                    mergeDestinationKeyPayload(child, itemEffectiveValues, itemPayload, itemReplicationIndices);
                }

                mappedItems.add(itemPayload);
            } else {
                mappedItems.add(rawItem);
            }
        }

        writePayloadValue(payload, replicatingContainer.getDestinationKey(), mappedItems, replicationIndices);
    }

    /**
     * Reconstructs the effective value of a replicating container from the payload.
     * <p>
     * Containers with their own destination key read an explicit list subtree and map each item
     * back into row-local effective values. Containers without a destination key infer their row
     * count from descendant wildcard paths and then read each row directly from the surrounding
     * payload. This preserves the same flexibility as the forward mapping where row data may either
     * live under a dedicated array node or be written into an externally owned array.
     */
    private void mergeReplicatingContainerEffectiveValues(@Nonnull ReplicatingContainerLayoutElement replicatingContainer,
                                                          @Nonnull Map<String, Object> payload,
                                                          @Nonnull EffectiveElementValues effectiveValues,
                                                          @Nonnull List<Integer> replicationIndices) {
        if (StringUtils.isNotNullOrEmpty(replicatingContainer.getDestinationKey())) {
            var rawValue = readPayloadValue(payload, replicatingContainer.getDestinationKey(), replicationIndices);
            if (rawValue == PAYLOAD_VALUE_NOT_FOUND) {
                return;
            }

            if (rawValue == null) {
                effectiveValues.put(replicatingContainer.getId(), null);
                return;
            }

            if (!(rawValue instanceof List<?> rawItems)) {
                effectiveValues.put(replicatingContainer.getId(), rawValue);
                return;
            }

            var mappedItems = new LinkedList<Object>();
            for (var itemIndex = 0; itemIndex < rawItems.size(); itemIndex++) {
                var rawItem = rawItems.get(itemIndex);
                if (rawItem instanceof Map<?, ?> rawItemMap) {
                    var itemPayload = toStringObjectMap(rawItemMap);
                    var itemEffectiveValues = new EffectiveElementValues();
                    var itemReplicationIndices = appendReplicationIndex(replicationIndices, itemIndex);

                    for (var child : replicatingContainer.getChildren()) {
                        mergeEffectiveValues(child, itemPayload, itemEffectiveValues, itemReplicationIndices);
                    }

                    mappedItems.add(itemEffectiveValues);
                } else {
                    mappedItems.add(rawItem);
                }
            }

            effectiveValues.put(replicatingContainer.getId(), mappedItems);
            return;
        }

        var rowCount = determineReplicationItemCount(replicatingContainer, payload, replicationIndices);
        if (rowCount <= 0) {
            return;
        }

        var mappedItems = new LinkedList<Object>();
        for (var itemIndex = 0; itemIndex < rowCount; itemIndex++) {
            var itemEffectiveValues = new EffectiveElementValues();
            var itemReplicationIndices = appendReplicationIndex(replicationIndices, itemIndex);

            for (var child : replicatingContainer.getChildren()) {
                mergeEffectiveValues(child, payload, itemEffectiveValues, itemReplicationIndices);
            }

            mappedItems.add(itemEffectiveValues);
        }

        effectiveValues.put(replicatingContainer.getId(), mappedItems);
    }

    /**
     * Writes a single input element value into the destination payload.
     * <p>
     * Inputs are only exported when both a destination key is configured and an effective value is
     * present. This keeps the generated payload aligned with explicit mapping intent and prevents
     * accidental leakage of internal-only elements.
     */
    private void mergeInputPayload(@Nonnull BaseInputElement<?> inputElement,
                                   @Nonnull Map<String, Object> effectiveValues,
                                   @Nonnull Map<String, Object> payload,
                                   @Nonnull List<Integer> replicationIndices) {
        if (StringUtils.isNullOrEmpty(inputElement.getDestinationKey()) || !effectiveValues.containsKey(inputElement.getId())) {
            return;
        }

        writePayloadValue(payload, inputElement.getDestinationKey(), effectiveValues.get(inputElement.getId()), replicationIndices);
    }

    /**
     * Reads a single input element value from the payload and stores it under the element id.
     * <p>
     * Missing payload paths are ignored so partial payloads do not create synthetic effective
     * values. An explicit {@code null} at the destination path is preserved because it carries
     * semantic meaning distinct from the absence of the path.
     */
    private void mergeInputEffectiveValue(@Nonnull BaseInputElement<?> inputElement,
                                          @Nonnull Map<String, Object> payload,
                                          @Nonnull EffectiveElementValues effectiveValues,
                                          @Nonnull List<Integer> replicationIndices) {
        if (StringUtils.isNullOrEmpty(inputElement.getDestinationKey())) {
            return;
        }

        var value = readPayloadValue(payload, inputElement.getDestinationKey(), replicationIndices);
        if (value == PAYLOAD_VALUE_NOT_FOUND) {
            return;
        }

        effectiveValues.put(inputElement.getId(), value);
    }

    /**
     * Parses a destination key and forwards it to the structural writer.
     * <p>
     * Wildcard substitution is performed before the actual write so downstream container handling
     * can work with a uniform segment model. Any wildcard that cannot be resolved from replication
     * context remains untouched and is later interpreted as a broadcast over an existing array.
     */
    private void writePayloadValue(@Nonnull Map<String, Object> payload,
                                   @Nullable String destinationKey,
                                   @Nullable Object value,
                                   @Nonnull List<Integer> replicationIndices) {
        if (StringUtils.isNullOrEmpty(destinationKey)) {
            return;
        }

        var segments = Arrays.stream(destinationKey.split("\\."))
                .map(String::trim)
                .filter(StringUtils::isNotNullOrEmpty)
                .toList();

        if (segments.isEmpty()) {
            return;
        }

        writePayloadValue(
                payload,
                substituteWildcardSegments(segments, replicationIndices),
                0,
                value
        );
    }

    /**
     * Resolves a destination key against the payload and returns the addressed value.
     * <p>
     * Known replication indices are substituted first so row-local reads target a concrete array
     * item. Any remaining wildcard is interpreted as a broadcast read and resolves to the first
     * matching value in the array, which mirrors the forward behavior where a single input value
     * may be written to all items of an existing array.
     */
    @Nullable
    private Object readPayloadValue(@Nonnull Map<String, Object> payload,
                                    @Nullable String destinationKey,
                                    @Nonnull List<Integer> replicationIndices) {
        if (StringUtils.isNullOrEmpty(destinationKey)) {
            return PAYLOAD_VALUE_NOT_FOUND;
        }

        var segments = Arrays.stream(destinationKey.split("\\."))
                .map(String::trim)
                .filter(StringUtils::isNotNullOrEmpty)
                .toList();

        if (segments.isEmpty()) {
            return PAYLOAD_VALUE_NOT_FOUND;
        }

        return readPayloadValue(payload, substituteWildcardSegments(segments, replicationIndices), 0);
    }

    /**
     * Recursively materializes the map/list structure addressed by the provided path segments.
     * <p>
     * This method supports three write modes:
     * <ul>
     *     <li>object traversal for regular path segments,</li>
     *     <li>indexed array access for numeric segments,</li>
     *     <li>array broadcast for unresolved {@code *} segments.</li>
     * </ul>
     * The method creates intermediate containers on demand because destination keys describe the
     * desired payload shape declaratively. That allows payload authors to specify deeply nested
     * targets without pre-initializing the entire structure elsewhere.
     */
    private void writePayloadValue(@Nonnull Object container,
                                   @Nonnull List<String> segments,
                                   int currentIndex,
                                   @Nullable Object value) {
        var segment = segments.get(currentIndex);
        var isLastSegment = currentIndex == segments.size() - 1;

        if (container instanceof Map<?, ?> currentMapContainer) {
            var currentMap = toStringObjectMap(currentMapContainer);
            if (isLastSegment) {
                currentMap.put(segment, value);
                return;
            }

            var nextSegment = segments.get(currentIndex + 1);
            var nextContainer = ensureChildContainer(currentMap.get(segment), nextSegment);
            currentMap.put(segment, nextContainer);
            writePayloadValue(nextContainer, segments, currentIndex + 1, value);
            return;
        }

        if (container instanceof List<?> currentListContainer) {
            var currentList = toMutableList(currentListContainer);

            if ("*".equals(segment)) {
                if (currentList.isEmpty()) {
                    return;
                }

                if (isLastSegment) {
                    Collections.fill(currentList, value);
                    return;
                }

                var nextSegment = segments.get(currentIndex + 1);
                for (var i = 0; i < currentList.size(); i++) {
                    var nextContainer = ensureChildContainer(currentList.get(i), nextSegment);
                    currentList.set(i, nextContainer);
                    writePayloadValue(nextContainer, segments, currentIndex + 1, value);
                }
                return;
            }

            if (!isArrayIndex(segment)) {
                return;
            }

            var arrayIndex = Integer.parseInt(segment);
            ensureListSize(currentList, arrayIndex + 1);
            if (isLastSegment) {
                currentList.set(arrayIndex, value);
                return;
            }

            var nextSegment = segments.get(currentIndex + 1);
            var nextContainer = ensureChildContainer(currentList.get(arrayIndex), nextSegment);
            currentList.set(arrayIndex, nextContainer);
            writePayloadValue(nextContainer, segments, currentIndex + 1, value);
        }
    }

    /**
     * Recursively resolves a value from a map/list payload structure.
     * <p>
     * Unresolved wildcard segments perform a fan-out read over the current array and return the
     * first matching value. This is sufficient for reverse-mapping broadcast values back into a
     * single effective value while remaining deterministic if all broadcast targets contain the
     * same data.
     */
    @Nullable
    private Object readPayloadValue(@Nullable Object container,
                                    @Nonnull List<String> segments,
                                    int currentIndex) {
        if (container == null) {
            return PAYLOAD_VALUE_NOT_FOUND;
        }

        var segment = segments.get(currentIndex);
        var isLastSegment = currentIndex == segments.size() - 1;

        if (container instanceof Map<?, ?> currentMap) {
            if (!(currentMap.containsKey(segment) && currentMap.get(segment) != PAYLOAD_VALUE_NOT_FOUND)) {
                return PAYLOAD_VALUE_NOT_FOUND;
            }

            var value = currentMap.get(segment);
            if (isLastSegment) {
                return value;
            }

            return readPayloadValue(value, segments, currentIndex + 1);
        }

        if (container instanceof List<?> currentList) {
            if ("*".equals(segment)) {
                for (var item : currentList) {
                    var value = isLastSegment
                            ? item
                            : readPayloadValue(item, segments, currentIndex + 1);
                    if (value != PAYLOAD_VALUE_NOT_FOUND) {
                        return value;
                    }
                }

                return PAYLOAD_VALUE_NOT_FOUND;
            }

            if (!isArrayIndex(segment)) {
                return PAYLOAD_VALUE_NOT_FOUND;
            }

            var index = Integer.parseInt(segment);
            if (index < 0 || index >= currentList.size()) {
                return PAYLOAD_VALUE_NOT_FOUND;
            }

            var value = currentList.get(index);
            if (isLastSegment) {
                return value;
            }

            return readPayloadValue(value, segments, currentIndex + 1);
        }

        return PAYLOAD_VALUE_NOT_FOUND;
    }

    /**
     * Replaces wildcard segments with the current replication indices when such context is
     * available.
     * <p>
     * Wildcards are resolved from left to right so nested replicating containers can naturally map
     * to nested arrays. Unresolved wildcards are intentionally preserved to enable later broadcast
     * semantics when the destination path refers to an already materialized array outside the
     * current replication context.
     */
    @Nonnull
    private List<String> substituteWildcardSegments(@Nonnull List<String> segments,
                                                    @Nonnull List<Integer> replicationIndices) {
        var resolvedSegments = new LinkedList<String>();
        var replicationIndexCursor = 0;

        for (var segment : segments) {
            if ("*".equals(segment) && replicationIndexCursor < replicationIndices.size()) {
                resolvedSegments.add(String.valueOf(replicationIndices.get(replicationIndexCursor)));
                replicationIndexCursor++;
            } else {
                resolvedSegments.add(segment);
            }
        }

        return resolvedSegments;
    }

    /**
     * Determines how many rows a destination payload provides for a replicating container without
     * its own destination key.
     * <p>
     * Such containers rely on descendant wildcard paths to address the external array. The row
     * count is therefore inferred from the first unresolved wildcard list reachable through any
     * descendant mapping path, taking the maximum count to avoid truncating partially populated
     * rows.
     */
    private int determineReplicationItemCount(@Nonnull BaseElement element,
                                              @Nonnull Map<String, Object> payload,
                                              @Nonnull List<Integer> replicationIndices) {
        if (element instanceof ReplicatingContainerLayoutElement replicatingContainer) {
            if (StringUtils.isNotNullOrEmpty(replicatingContainer.getDestinationKey())) {
                var rawValue = readPayloadValue(payload, replicatingContainer.getDestinationKey(), replicationIndices);
                if (rawValue instanceof List<?> rawItems) {
                    return rawItems.size();
                }

                return rawValue == PAYLOAD_VALUE_NOT_FOUND ? -1 : 1;
            }

            var maxItemCount = -1;
            for (var child : replicatingContainer.getChildren()) {
                maxItemCount = Math.max(maxItemCount, determineReplicationItemCount(child, payload, replicationIndices));
            }
            return maxItemCount;
        }

        if (element instanceof BaseInputElement<?> inputElement) {
            return determineWildcardArraySize(payload, inputElement.getDestinationKey(), replicationIndices);
        }

        if (element instanceof LayoutElement<?> layoutElement) {
            var maxItemCount = -1;
            for (var child : layoutElement.getChildren()) {
                maxItemCount = Math.max(maxItemCount, determineReplicationItemCount(child, payload, replicationIndices));
            }
            return maxItemCount;
        }

        return -1;
    }

    /**
     * Returns the size of the first unresolved wildcard array in the given destination key.
     * <p>
     * This is used to infer replication row counts from payload paths that are anchored outside the
     * current container.
     */
    private int determineWildcardArraySize(@Nonnull Map<String, Object> payload,
                                           @Nullable String destinationKey,
                                           @Nonnull List<Integer> replicationIndices) {
        if (StringUtils.isNullOrEmpty(destinationKey)) {
            return -1;
        }

        var segments = substituteWildcardSegments(
                Arrays.stream(destinationKey.split("\\."))
                        .map(String::trim)
                        .filter(StringUtils::isNotNullOrEmpty)
                        .toList(),
                replicationIndices
        );

        Object current = payload;
        for (var segment : segments) {
            if ("*".equals(segment)) {
                return current instanceof List<?> currentList ? currentList.size() : -1;
            }

            if (current instanceof Map<?, ?> currentMap) {
                if (!currentMap.containsKey(segment)) {
                    return -1;
                }
                current = currentMap.get(segment);
                continue;
            }

            if (current instanceof List<?> currentList) {
                if (!isArrayIndex(segment)) {
                    return -1;
                }

                var index = Integer.parseInt(segment);
                if (index < 0 || index >= currentList.size()) {
                    return -1;
                }

                current = currentList.get(index);
                continue;
            }

            return -1;
        }

        return -1;
    }

    /**
     * Extends the current replication context with the index of the row currently being processed.
     * <p>
     * The context is represented as an ordered list so nested replicating containers can each
     * consume one wildcard segment without sharing mutable traversal state.
     */
    @Nonnull
    private List<Integer> appendReplicationIndex(@Nonnull List<Integer> replicationIndices,
                                                 int replicationIndex) {
        var result = new LinkedList<>(replicationIndices);
        result.add(replicationIndex);
        return result;
    }

    /**
     * Chooses or creates the intermediate container needed for the next path segment.
     * <p>
     * The decision is driven by the upcoming segment rather than the current one because the next
     * write target determines whether the current node must behave like an object or an array.
     * This allows the writer to construct the destination structure lazily while still honoring the
     * semantics encoded in the destination key.
     */
    @Nonnull
    private Object ensureChildContainer(@Nullable Object currentValue,
                                        @Nonnull String nextSegment) {
        if (isArraySegment(nextSegment)) {
            if (currentValue instanceof List<?> currentList) {
                return toMutableList(currentList);
            }

            return new LinkedList<>();
        }

        if (currentValue instanceof Map<?, ?> currentMap) {
            return toStringObjectMap(currentMap);
        }

        return new LinkedHashMap<String, Object>();
    }

    /**
     * Returns whether the path segment denotes array traversal.
     * <p>
     * Both explicit indices and wildcards are treated as array segments because they imply list
     * semantics for intermediate container creation.
     */
    private boolean isArraySegment(@Nullable String segment) {
        return "*".equals(segment) || isArrayIndex(segment);
    }

    /**
     * Returns whether the segment addresses a concrete array index.
     * <p>
     * Restricting indexed access to unsigned integer segments keeps destination-key parsing
     * unambiguous and mirrors the way array positions are represented in JSON-like payloads.
     */
    private boolean isArrayIndex(@Nullable String segment) {
        return segment != null && segment.matches("\\d+");
    }

    /**
     * Enlarges the target list so indexed writes can safely address sparse positions.
     * <p>
     * Missing slots are filled with {@code null} because an indexed destination key expresses
     * positional intent. Preserving the requested index is more important than compacting the array
     * and shifting later entries.
     */
    private void ensureListSize(@Nonnull List<Object> list, int size) {
        while (list.size() < size) {
            list.add(null);
        }
    }

    /**
     * Returns a mutable list instance for structural writes.
     * <p>
     * Existing mutable lists are reused so in-place updates remain visible to the parent payload.
     * Other list implementations are copied because the writer may need to resize or replace
     * elements while constructing the destination structure.
     */
    @Nonnull
    private List<Object> toMutableList(@Nonnull List<?> source) {
        if (source instanceof LinkedList<?> existingList) {
            @SuppressWarnings("unchecked")
            var mutableList = (List<Object>) existingList;
            return mutableList;
        }

        return new LinkedList<>(source);
    }

    /**
     * Returns a string-keyed map that can be safely mutated by the writer.
     * <p>
     * Reusing existing {@link LinkedHashMap} instances preserves insertion order and keeps changes
     * attached to the parent payload, while copying other map implementations normalizes key types
     * and shields the writer from immutable or non-string-keyed inputs.
     */
    @Nonnull
    private LinkedHashMap<String, Object> toStringObjectMap(@Nonnull Map<?, ?> source) {
        if (source instanceof LinkedHashMap<?, ?> existingMap) {
            @SuppressWarnings("unchecked")
            var mutableMap = (LinkedHashMap<String, Object>) existingMap;
            return mutableMap;
        }

        var result = new LinkedHashMap<String, Object>();
        source.forEach((key, value) -> {
            if (key instanceof String stringKey) {
                result.put(stringKey, value);
            }
        });
        return result;
    }
}
