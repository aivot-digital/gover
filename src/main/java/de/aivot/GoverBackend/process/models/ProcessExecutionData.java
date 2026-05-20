package de.aivot.GoverBackend.process.models;

import de.aivot.GoverBackend.elements.models.EffectiveElementValues;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.submission.services.ElementDataTransformService;
import de.aivot.GoverBackend.utils.MapUtils;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the data structure used to hold process instance data during execution. It extends HashMap to allow dynamic storage of key-value pairs.
 * <p>
 * The following keys are always set:
 * <ul>
 * <li><strong>$</strong>: The process instance data itself. E.g. "$.applicant.name" gives access to the applicant's name.</li>
 * <li><strong>$$</strong>: The process instance metadata. E.g. "$$.identities" gives access to the identities of the process instance.</li>
 * <li><strong>_</strong>: The map of all process node results. E.g. "_.<nodeDataKey>" gives access to the result of the node with the given "nodeDataKey".</li>
 * </ul>
 */
public class ProcessExecutionData extends HashMap<String, Object> {
    public static final String PROCESS_DATA_KEY = "$";
    public static final String PROCESS_METADATA_KEY = "$$";
    public static final String NODE_RESULTS_KEY = "_";

    public static final String[] PROCESS_EXEC_DATA_KEYS = {
            PROCESS_DATA_KEY,
            PROCESS_METADATA_KEY,
            NODE_RESULTS_KEY
    };

    public ProcessExecutionData() {
        put(PROCESS_DATA_KEY, new HashMap<String, Object>());
        put(PROCESS_METADATA_KEY, new HashMap<String, Object>());
        put(NODE_RESULTS_KEY, new HashMap<String, Object>());
    }

    public Map<String, Object> getProcessData() {
        var res = (Map<String, Object>) this.get(PROCESS_DATA_KEY);
        if (res == null) {
            res = new HashMap<>();
            this.put(PROCESS_DATA_KEY, res);
        }
        return res;
    }

    public Map<String, Object> getNodeData() {
        var res = (Map<String, Object>) this.get(NODE_RESULTS_KEY);
        if (res == null) {
            res = new HashMap<>();
            this.put(NODE_RESULTS_KEY, res);
        }
        return res;
    }

    public static ProcessExecutionData of(Map<String, Object> data) {
        ProcessExecutionData executionData = new ProcessExecutionData();
        executionData.putAll(data);
        return executionData;
    }

    /**
     * Resolves a value from the process-data root ({@code $}) using destination-key syntax.
     *
     * <p>The path language is intentionally limited to destination-key semantics: segments are separated by dots,
     * numeric segments address array indices, and {@code *} resolves to the first entry of the current array unless
     * explicit wildcard indices are provided via
     * {@link #resolveProcessDataValue(ProcessExecutionData, String, List)}. Bracket notation is not supported.
     */
    @Nullable
    public static Object resolveProcessDataValue(@Nonnull ProcessExecutionData processExecutionData,
                                                 @Nullable String destinationKey) {
        return resolveProcessDataValue(processExecutionData, destinationKey, null);
    }

    /**
     * Resolves a value from the process-data root ({@code $}) using destination-key syntax and optional wildcard
     * indices.
     *
     * <p>Each {@code *} segment consumes one entry from {@code wildcardIndices} from left to right. For example,
     * {@code personen.*.adressen.*.strasse} with {@code [1, 2]} resolves
     * {@code personen.1.adressen.2.strasse}. If no wildcard indices are provided, each {@code *} resolves to
     * index {@code 0}.
     */
    @Nullable
    public static Object resolveProcessDataValue(@Nonnull ProcessExecutionData processExecutionData,
                                                 @Nullable String destinationKey,
                                                 @Nullable List<Integer> wildcardIndices) {
        var segments = parseDestinationKeySegments(destinationKey);
        if (segments.isEmpty()) {
            return processExecutionData.get(PROCESS_DATA_KEY);
        }

        var validatedWildcardIndices = validateReadWildcardIndices(segments, wildcardIndices);
        return resolvePathValue(
                processExecutionData.get(PROCESS_DATA_KEY),
                segments,
                0,
                validatedWildcardIndices,
                new int[]{0}
        );
    }

    /**
     * Writes a value into the process-data root ({@code $}) using destination-key syntax.
     *
     * <p>Array segments are expressed either as numeric path segments such as {@code personen.1.vorname} or as the
     * wildcard segment {@code *}. A wildcard without explicit indices broadcasts over all currently existing array
     * items. Use {@link #writeProcessDataValue(ProcessExecutionData, String, Object, Collection)} to target only a
     * subset of array items for one wildcard segment.
     */
    public static void writeProcessDataValue(@Nonnull ProcessExecutionData processExecutionData,
                                             @Nullable String destinationKey,
                                             @Nullable Object value) {
        writeProcessDataValue(processExecutionData, destinationKey, value, null);
    }

    /**
     * Writes a value into the process-data root ({@code $}) using destination-key syntax and optional explicit
     * wildcard targets.
     *
     * <p>If {@code wildcardIndices} is provided, the path must contain exactly one wildcard segment. That wildcard
     * is then expanded to the given indices. Missing list slots are created as needed and filled with {@code null}
     * until the requested index can be written.
     */
    public static void writeProcessDataValue(@Nonnull ProcessExecutionData processExecutionData,
                                             @Nullable String destinationKey,
                                             @Nullable Object value,
                                             @Nullable Collection<Integer> wildcardIndices) {
        var segments = parseDestinationKeySegments(destinationKey);
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("Destination key must not be empty when writing process data.");
        }

        validateWildcardTargeting(segments, wildcardIndices);

        var processDataRoot = ensureMutableProcessDataRoot(processExecutionData);
        writePathValue(
                processDataRoot,
                segments,
                0,
                value,
                wildcardIndices != null ? List.copyOf(wildcardIndices) : null
        );
    }

    public ProcessExecutionData addProcessData(Object processData) {
        this.put(PROCESS_DATA_KEY, processData);
        return this;
    }

    public ProcessExecutionData addProcessData(String key, Object value) {
        this.putIfAbsent(PROCESS_DATA_KEY, new HashMap<String, Object>());

        @SuppressWarnings("unchecked")
        HashMap<String, Object> dataMap = (HashMap<String, Object>) this.get(PROCESS_DATA_KEY);
        dataMap.put(key, value);

        return this;
    }

    public ProcessExecutionData patchWithElementData(ElementDataTransformService elementDataTransformService,
                                                     BaseElement baseElement,
                                                     EffectiveElementValues effectiveElementValues) {
        var clone = new ProcessExecutionData();
        clone.putAll(MapUtils.deepCopy(this));

        @SuppressWarnings("unchecked")
        Map<String, Object> processData = (Map<String, Object>) clone.get(PROCESS_DATA_KEY);
        if (processData == null) {
            processData = new HashMap<>();
        }

        var d = elementDataTransformService
                .buildPayload(
                        baseElement,
                        effectiveElementValues,
                        processData
                );
        clone.put(PROCESS_DATA_KEY, d);

        return clone;
    }

    public ProcessExecutionData addProcessMetadata(Object processMetadata) {
        this.put(PROCESS_METADATA_KEY, processMetadata);
        return this;
    }

    private ProcessExecutionData addProcessMetadata(String key, Object value) {
        this.putIfAbsent(PROCESS_METADATA_KEY, new HashMap<String, Object>());

        @SuppressWarnings("unchecked")
        HashMap<String, Object> metadataMap = (HashMap<String, Object>) this.get(PROCESS_METADATA_KEY);
        metadataMap.put(key, value);

        return this;
    }

    public ProcessExecutionData addNodeResults(ProcessNodeEntity node, Object nodeResults) {
        this.putIfAbsent(NODE_RESULTS_KEY, new HashMap<String, Object>());

        @SuppressWarnings("unchecked")
        HashMap<String, Object> allNodeResults = (HashMap<String, Object>) this.get(NODE_RESULTS_KEY);
        allNodeResults.put(node.getDataKey(), nodeResults);

        return this;
    }

    @Nonnull
    private static List<String> parseDestinationKeySegments(@Nullable String destinationKey) {
        if (StringUtils.isNullOrEmpty(destinationKey)) {
            return List.of();
        }

        var normalizedKey = destinationKey.trim();
        var segments = Arrays.stream(normalizedKey.split("\\.", -1))
                .map(String::trim)
                .toList();

        if (segments.stream().anyMatch(StringUtils::isNullOrEmpty)) {
            throw new IllegalArgumentException("Destination key contains an empty path segment.");
        }

        for (var segment : segments) {
            if (segment.contains("[") || segment.contains("]")) {
                throw new IllegalArgumentException("Destination keys do not support bracket array syntax.");
            }
            if (!"*".equals(segment) && segment.contains("*")) {
                throw new IllegalArgumentException("The wildcard '*' must be used as its own destination-key segment.");
            }
        }

        if (isArraySegment(segments.getFirst())) {
            throw new IllegalArgumentException("Destination key must start with an object segment.");
        }

        return segments;
    }

    private static void validateWildcardTargeting(@Nonnull List<String> segments,
                                                  @Nullable Collection<Integer> wildcardIndices) {
        if (wildcardIndices == null) {
            return;
        }

        var wildcardCount = segments.stream().filter("*"::equals).count();
        if (wildcardCount == 0) {
            throw new IllegalArgumentException("Wildcard indices can only be provided for destination keys that contain '*'.");
        }
        if (wildcardCount > 1) {
            throw new IllegalArgumentException("Wildcard index targeting currently supports exactly one wildcard segment.");
        }

        for (var index : wildcardIndices) {
            if (index == null || index < 0) {
                throw new IllegalArgumentException("Wildcard indices must be non-negative integers.");
            }
        }
    }

    @Nullable
    private static List<Integer> validateReadWildcardIndices(@Nonnull List<String> segments,
                                                             @Nullable List<Integer> wildcardIndices) {
        if (wildcardIndices == null) {
            return null;
        }

        var wildcardCount = segments.stream().filter("*"::equals).count();
        if (wildcardCount != wildcardIndices.size()) {
            throw new IllegalArgumentException("Wildcard index count must match the number of '*' segments in the destination key.");
        }

        for (var index : wildcardIndices) {
            if (index == null || index < 0) {
                throw new IllegalArgumentException("Wildcard indices must be non-negative integers.");
            }
        }

        return List.copyOf(wildcardIndices);
    }

    @Nullable
    private static Object resolvePathValue(@Nullable Object container,
                                           @Nonnull List<String> segments,
                                           int currentIndex,
                                           @Nullable List<Integer> wildcardIndices,
                                           @Nonnull int[] wildcardIndexCursor) {
        if (container == null) {
            return null;
        }

        var segment = segments.get(currentIndex);
        var isLastSegment = currentIndex == segments.size() - 1;

        if (container instanceof Map<?, ?> currentMap) {
            if (!currentMap.containsKey(segment)) {
                return null;
            }

            var value = currentMap.get(segment);
            if (isLastSegment) {
                return value;
            }

            return resolvePathValue(value, segments, currentIndex + 1, wildcardIndices, wildcardIndexCursor);
        }

        if (!(container instanceof List<?> currentList)) {
            return null;
        }

        var arrayIndex = resolveReadArrayIndex(segment, wildcardIndices, wildcardIndexCursor);
        if (arrayIndex == null || arrayIndex < 0 || arrayIndex >= currentList.size()) {
            return null;
        }

        var value = currentList.get(arrayIndex);
        if (isLastSegment) {
            return value;
        }

        return resolvePathValue(value, segments, currentIndex + 1, wildcardIndices, wildcardIndexCursor);
    }

    private static void writePathValue(@Nonnull Object container,
                                       @Nonnull List<String> segments,
                                       int currentIndex,
                                       @Nullable Object value,
                                       @Nullable List<Integer> wildcardIndices) {
        var segment = segments.get(currentIndex);
        var isLastSegment = currentIndex == segments.size() - 1;

        if (container instanceof Map<?, ?> currentMapContainer) {
            var currentMap = toMutableStringObjectMap(currentMapContainer);
            if (isLastSegment) {
                currentMap.put(segment, value);
                return;
            }

            var nextSegment = segments.get(currentIndex + 1);
            var nextContainer = ensureChildContainer(currentMap.get(segment), nextSegment);
            currentMap.put(segment, nextContainer);
            writePathValue(nextContainer, segments, currentIndex + 1, value, wildcardIndices);
            return;
        }

        if (!(container instanceof List<?> currentListContainer)) {
            throw new IllegalStateException("Destination key expects an array segment, but the current value is not a list.");
        }

        var currentList = toMutableList(currentListContainer);
        if ("*".equals(segment)) {
            writeWildcardPathValue(currentList, segments, currentIndex, value, wildcardIndices);
            return;
        }

        var arrayIndex = resolveWriteArrayIndex(segment);
        if (arrayIndex == null) {
            throw new IllegalStateException("Destination key expects an array index, but the current segment is not numeric.");
        }

        ensureListSize(currentList, arrayIndex + 1);
        if (isLastSegment) {
            currentList.set(arrayIndex, value);
            return;
        }

        var nextSegment = segments.get(currentIndex + 1);
        var nextContainer = ensureChildContainer(currentList.get(arrayIndex), nextSegment);
        currentList.set(arrayIndex, nextContainer);
        writePathValue(nextContainer, segments, currentIndex + 1, value, wildcardIndices);
    }

    private static void writeWildcardPathValue(@Nonnull List<Object> currentList,
                                               @Nonnull List<String> segments,
                                               int currentIndex,
                                               @Nullable Object value,
                                               @Nullable List<Integer> wildcardIndices) {
        var isLastSegment = currentIndex == segments.size() - 1;
        var targetIndices = wildcardIndices != null
                ? wildcardIndices
                : createBroadcastIndices(currentList.size());

        if (targetIndices.isEmpty()) {
            return;
        }

        if (wildcardIndices != null) {
            var maxIndex = targetIndices.stream().max(Integer::compareTo).orElse(-1);
            ensureListSize(currentList, maxIndex + 1);
        } else if (currentList.isEmpty()) {
            return;
        }

        if (isLastSegment) {
            for (var targetIndex : targetIndices) {
                currentList.set(targetIndex, value);
            }
            return;
        }

        var nextSegment = segments.get(currentIndex + 1);
        for (var targetIndex : targetIndices) {
            var nextContainer = ensureChildContainer(currentList.get(targetIndex), nextSegment);
            currentList.set(targetIndex, nextContainer);
            writePathValue(nextContainer, segments, currentIndex + 1, value, null);
        }
    }

    @Nonnull
    private static Map<String, Object> ensureMutableProcessDataRoot(@Nonnull ProcessExecutionData processExecutionData) {
        var root = processExecutionData.get(PROCESS_DATA_KEY);
        if (root == null) {
            var mutableRoot = new LinkedHashMap<String, Object>();
            processExecutionData.put(PROCESS_DATA_KEY, mutableRoot);
            return mutableRoot;
        }

        if (!(root instanceof Map<?, ?> rootMap)) {
            throw new IllegalStateException("ProcessExecutionData.$ must be an object.");
        }

        var mutableRoot = toMutableStringObjectMap(rootMap);
        if (mutableRoot != root) {
            processExecutionData.put(PROCESS_DATA_KEY, mutableRoot);
        }
        return mutableRoot;
    }

    @Nonnull
    private static Object ensureChildContainer(@Nullable Object currentValue,
                                               @Nonnull String nextSegment) {
        if (currentValue == null) {
            return isArraySegment(nextSegment)
                    ? new ArrayList<>()
                    : new LinkedHashMap<String, Object>();
        }

        if (isArraySegment(nextSegment)) {
            if (currentValue instanceof List<?> currentList) {
                return toMutableList(currentList);
            }

            throw new IllegalStateException("Destination key expects an array, but an incompatible value already exists.");
        }

        if (currentValue instanceof Map<?, ?> currentMap) {
            return toMutableStringObjectMap(currentMap);
        }

        throw new IllegalStateException("Destination key expects an object, but an incompatible value already exists.");
    }

    private static boolean isArraySegment(@Nullable String segment) {
        return "*".equals(segment) || resolveWriteArrayIndex(segment) != null;
    }

    @Nullable
    private static Integer resolveReadArrayIndex(@Nullable String segment,
                                                 @Nullable List<Integer> wildcardIndices,
                                                 @Nonnull int[] wildcardIndexCursor) {
        if ("*".equals(segment)) {
            if (wildcardIndices != null) {
                return wildcardIndices.get(wildcardIndexCursor[0]++);
            }
            return 0;
        }

        return resolveWriteArrayIndex(segment);
    }

    @Nullable
    private static Integer resolveWriteArrayIndex(@Nullable String segment) {
        if (segment == null || !segment.matches("\\d+")) {
            return null;
        }

        return Integer.parseInt(segment);
    }

    private static void ensureListSize(@Nonnull List<Object> list, int size) {
        while (list.size() < size) {
            list.add(null);
        }
    }

    @Nonnull
    private static List<Integer> createBroadcastIndices(int size) {
        var result = new ArrayList<Integer>(size);
        for (var i = 0; i < size; i++) {
            result.add(i);
        }
        return result;
    }

    @Nonnull
    private static List<Object> toMutableList(@Nonnull List<?> source) {
        if (source instanceof ArrayList<?> existingList) {
            @SuppressWarnings("unchecked")
            var mutableList = (List<Object>) existingList;
            return mutableList;
        }

        return new ArrayList<>(source);
    }

    @Nonnull
    private static LinkedHashMap<String, Object> toMutableStringObjectMap(@Nonnull Map<?, ?> source) {
        if (source instanceof LinkedHashMap<?, ?> existingMap) {
            @SuppressWarnings("unchecked")
            var mutableMap = (LinkedHashMap<String, Object>) existingMap;
            return mutableMap;
        }

        var result = new LinkedHashMap<String, Object>();
        source.forEach((key, entryValue) -> {
            if (key instanceof String stringKey) {
                result.put(stringKey, entryValue);
            }
        });
        return result;
    }
}
