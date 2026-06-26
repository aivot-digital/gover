package de.aivot.gover.backend.process.models;

import de.aivot.gover.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for destination-key based access to process execution data and compatible object graphs.
 */
public final class ProcessDataValueUtils {
    private ProcessDataValueUtils() {
    }

    public record ResolvedDestinationKeyValue(@Nonnull String destinationKey,
                                              @Nonnull List<Integer> wildcardIndices,
                                              @Nullable Object value) {
        public ResolvedDestinationKeyValue {
            wildcardIndices = List.copyOf(wildcardIndices);
        }
    }

    public record ResolvedProcessDataValue(@Nonnull String destinationKey,
                                           @Nonnull List<Integer> wildcardIndices,
                                           @Nullable Object value) {
        public ResolvedProcessDataValue {
            wildcardIndices = List.copyOf(wildcardIndices);
        }
    }

    private record DestinationKeyRemovalResult(@Nullable Object container,
                                               boolean removed,
                                               boolean containerEmpty) {
        private static final DestinationKeyRemovalResult NOT_FOUND = new DestinationKeyRemovalResult(null, false, false);
    }

    /**
     * Resolves a value from the process-data root ({@code $}) using destination-key syntax.
     *
     * <p>Wildcard paths require explicit indices. Use
     * {@link #resolveProcessDataValue(ProcessExecutionData, String, List)} to resolve one concrete wildcard binding,
     * or {@link #resolveMatchingProcessDataValues(ProcessExecutionData, String)} to enumerate all existing wildcard
     * bindings. Bracket notation is not supported.
     */
    @Nullable
    public static Object resolveProcessDataValue(@Nonnull ProcessExecutionData processExecutionData,
                                                 @Nullable String destinationKey) {
        var segments = parseDestinationKeySegments(destinationKey, false);
        assertNoImplicitWildcards(segments, "resolve");
        return resolveDestinationKeyValue(processExecutionData.get(ProcessExecutionData.PROCESS_DATA_KEY), destinationKey);
    }

    /**
     * Resolves a value from the process-data root ({@code $}) using destination-key syntax and explicit wildcard
     * indices.
     *
     * <p>Each {@code *} segment consumes one entry from {@code wildcardIndices} from left to right. For example,
     * {@code personen.*.adressen.*.strasse} with {@code [1, 2]} resolves
     * {@code personen.1.adressen.2.strasse}.
     */
    @Nullable
    public static Object resolveProcessDataValue(@Nonnull ProcessExecutionData processExecutionData,
                                                 @Nullable String destinationKey,
                                                 @Nullable List<Integer> wildcardIndices) {
        parseDestinationKeySegments(destinationKey, false);
        return resolveDestinationKeyValue(processExecutionData.get(ProcessExecutionData.PROCESS_DATA_KEY), destinationKey, wildcardIndices);
    }

    /**
     * Resolves all currently addressable wildcard bindings for the given destination key.
     *
     * <p>For example, {@code personen.*.alter} returns one entry for every existing person array index. Each result
     * contains the concrete destination key, the wildcard index tuple used for that concrete key, and the resolved
     * value, which may be {@code null}.
     */
    @Nonnull
    public static List<ResolvedProcessDataValue> resolveMatchingProcessDataValues(@Nonnull ProcessExecutionData processExecutionData,
                                                                                  @Nullable String destinationKey) {
        parseDestinationKeySegments(destinationKey, false);
        return resolveMatchingDestinationKeyValues(processExecutionData.get(ProcessExecutionData.PROCESS_DATA_KEY), destinationKey)
                .stream()
                .map(result -> new ResolvedProcessDataValue(
                        result.destinationKey(),
                        result.wildcardIndices(),
                        result.value()
                ))
                .toList();
    }

    /**
     * Resolves a value from an arbitrary destination-key root. The root may be an object, array or scalar value.
     *
     * <p>Wildcard paths require explicit indices. Use
     * {@link #resolveDestinationKeyValue(Object, String, List)} to resolve one concrete wildcard binding or
     * {@link #resolveMatchingDestinationKeyValues(Object, String)} to enumerate all current bindings.
     */
    @Nullable
    public static Object resolveDestinationKeyValue(@Nullable Object root,
                                                    @Nullable String destinationKey) {
        var segments = parseDestinationKeySegments(destinationKey, true);
        assertNoImplicitWildcards(segments, "resolve");
        return resolveDestinationKeyValue(root, destinationKey, null);
    }

    /**
     * Resolves a value from an arbitrary destination-key root using explicit wildcard indices.
     */
    @Nullable
    public static Object resolveDestinationKeyValue(@Nullable Object root,
                                                    @Nullable String destinationKey,
                                                    @Nullable List<Integer> wildcardIndices) {
        var segments = parseDestinationKeySegments(destinationKey, true);
        if (segments.isEmpty()) {
            if (wildcardIndices != null && !wildcardIndices.isEmpty()) {
                throw new IllegalArgumentException("Wildcard indices can only be provided for destination keys that contain '*'.");
            }
            return root;
        }

        var validatedWildcardIndices = validateExplicitWildcardIndices(segments, wildcardIndices);
        return resolvePathValue(
                root,
                segments,
                0,
                validatedWildcardIndices,
                new int[]{0}
        );
    }

    /**
     * Resolves all currently addressable wildcard bindings from an arbitrary destination-key root.
     */
    @Nonnull
    public static List<ResolvedDestinationKeyValue> resolveMatchingDestinationKeyValues(@Nullable Object root,
                                                                                        @Nullable String destinationKey) {
        var segments = parseDestinationKeySegments(destinationKey, true);
        if (segments.isEmpty()) {
            return List.of(new ResolvedDestinationKeyValue("", List.of(), root));
        }

        var wildcardCount = countWildcardSegments(segments);
        if (wildcardCount == 0) {
            return List.of(new ResolvedDestinationKeyValue(
                    destinationKey != null ? destinationKey.trim() : "",
                    List.of(),
                    resolveDestinationKeyValue(root, destinationKey)
            ));
        }

        var wildcardIndexTuples = new ArrayList<List<Integer>>();
        collectWildcardIndexTuples(
                root,
                segments,
                0,
                new ArrayList<>(),
                wildcardIndexTuples
        );

        var results = new ArrayList<ResolvedDestinationKeyValue>(wildcardIndexTuples.size());
        for (var wildcardIndexTuple : wildcardIndexTuples) {
            results.add(new ResolvedDestinationKeyValue(
                    materializeDestinationKey(destinationKey, wildcardIndexTuple, true),
                    wildcardIndexTuple,
                    resolveDestinationKeyValue(root, destinationKey, wildcardIndexTuple)
            ));
        }
        return List.copyOf(results);
    }

    /**
     * Replaces every wildcard segment in a destination key with the corresponding explicit wildcard index.
     */
    @Nonnull
    public static String materializeDestinationKey(@Nullable String destinationKey,
                                                   @Nullable List<Integer> wildcardIndices) {
        return materializeDestinationKey(destinationKey, wildcardIndices, false);
    }

    @Nonnull
    public static String materializeDestinationKey(@Nullable String destinationKey,
                                                   @Nullable List<Integer> wildcardIndices,
                                                   boolean allowArrayRoot) {
        var segments = parseDestinationKeySegments(destinationKey, allowArrayRoot);
        if (segments.isEmpty()) {
            if (wildcardIndices != null && !wildcardIndices.isEmpty()) {
                throw new IllegalArgumentException("Wildcard indices can only be provided for destination keys that contain '*'.");
            }
            return "";
        }

        var validatedWildcardIndices = validateExplicitWildcardIndices(segments, wildcardIndices);
        var concreteSegments = new ArrayList<String>(segments.size());
        var wildcardIndexCursor = 0;
        for (var segment : segments) {
            if ("*".equals(segment)) {
                concreteSegments.add(Integer.toString(validatedWildcardIndices.get(wildcardIndexCursor++)));
            } else {
                concreteSegments.add(segment);
            }
        }
        return String.join(".", concreteSegments);
    }

    public static void validateDestinationKey(@Nullable String destinationKey) {
        validateDestinationKey(destinationKey, false);
    }

    public static void validateDestinationKey(@Nullable String destinationKey,
                                              boolean allowArrayRoot) {
        parseDestinationKeySegments(destinationKey, allowArrayRoot);
    }

    public static int countWildcardSegments(@Nullable String destinationKey) {
        return countWildcardSegments(destinationKey, false);
    }

    public static int countWildcardSegments(@Nullable String destinationKey,
                                            boolean allowArrayRoot) {
        return countWildcardSegments(parseDestinationKeySegments(destinationKey, allowArrayRoot));
    }

    public static boolean hasWildcardSegment(@Nullable String destinationKey) {
        return hasWildcardSegment(destinationKey, false);
    }

    public static boolean hasWildcardSegment(@Nullable String destinationKey,
                                             boolean allowArrayRoot) {
        return countWildcardSegments(destinationKey, allowArrayRoot) > 0;
    }

    /**
     * Writes a value into the process-data root ({@code $}) using destination-key syntax.
     *
     * <p>Wildcard paths require explicit indices. Use
     * {@link #writeProcessDataValue(ProcessExecutionData, String, Object, List)} for one concrete wildcard binding.
     */
    public static void writeProcessDataValue(@Nonnull ProcessExecutionData processExecutionData,
                                             @Nullable String destinationKey,
                                             @Nullable Object value) {
        var segments = parseDestinationKeySegments(destinationKey, false);
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("Destination key must not be empty when writing process data.");
        }
        assertNoImplicitWildcards(segments, "write");
        processExecutionData.put(
                ProcessExecutionData.PROCESS_DATA_KEY,
                writeDestinationKeyValue(processExecutionData.get(ProcessExecutionData.PROCESS_DATA_KEY), destinationKey, value)
        );
    }

    /**
     * Writes a value into the process-data root ({@code $}) using destination-key syntax and explicit wildcard
     * indices.
     *
     * <p>Each {@code *} segment consumes one entry from {@code wildcardIndices} from left to right. Missing list
     * slots are created as needed and filled with {@code null} until the requested index can be written.
     */
    public static void writeProcessDataValue(@Nonnull ProcessExecutionData processExecutionData,
                                             @Nullable String destinationKey,
                                             @Nullable Object value,
                                             @Nullable List<Integer> wildcardIndices) {
        var segments = parseDestinationKeySegments(destinationKey, false);
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("Destination key must not be empty when writing process data.");
        }
        processExecutionData.put(
                ProcessExecutionData.PROCESS_DATA_KEY,
                writeDestinationKeyValue(processExecutionData.get(ProcessExecutionData.PROCESS_DATA_KEY), destinationKey, value, wildcardIndices)
        );
    }

    /**
     * Removes a value from the process-data root ({@code $}) using destination-key syntax.
     *
     * <p>Wildcard segments remove all currently matching values. Array elements are removed from the list, so later
     * indices shift left. When {@code cleanupEmptyContainers} is enabled, empty maps and lists created by the removal
     * are pruned recursively.
     *
     * @return {@code true} if at least one value was removed.
     */
    public static boolean removeProcessDataValue(@Nonnull ProcessExecutionData processExecutionData,
                                                 @Nullable String destinationKey,
                                                 boolean cleanupEmptyContainers) {
        var segments = parseDestinationKeySegments(destinationKey, false);
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("Destination key must not be empty when removing process data.");
        }

        var currentRoot = processExecutionData.get(ProcessExecutionData.PROCESS_DATA_KEY);
        if (currentRoot == null) {
            return false;
        }

        if (!(currentRoot instanceof Map<?, ?>)) {
            return false;
        }

        var removalResult = removePathValue(
                currentRoot,
                segments,
                0,
                cleanupEmptyContainers,
                null,
                new int[]{0}
        );
        processExecutionData.put(ProcessExecutionData.PROCESS_DATA_KEY, removalResult.container());
        return removalResult.removed();
    }

    /**
     * Removes one concrete wildcard binding from the process-data root ({@code $}) using explicit wildcard indices.
     *
     * @return {@code true} if the bound value existed and was removed.
     */
    public static boolean removeProcessDataValue(@Nonnull ProcessExecutionData processExecutionData,
                                                 @Nullable String destinationKey,
                                                 boolean cleanupEmptyContainers,
                                                 @Nullable List<Integer> wildcardIndices) {
        var segments = parseDestinationKeySegments(destinationKey, false);
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("Destination key must not be empty when removing process data.");
        }

        var validatedWildcardIndices = validateExplicitWildcardIndices(segments, wildcardIndices);
        var currentRoot = processExecutionData.get(ProcessExecutionData.PROCESS_DATA_KEY);
        if (currentRoot == null) {
            return false;
        }

        if (!(currentRoot instanceof Map<?, ?>)) {
            return false;
        }

        var removalResult = removePathValue(
                currentRoot,
                segments,
                0,
                cleanupEmptyContainers,
                validatedWildcardIndices,
                new int[]{0}
        );
        processExecutionData.put(ProcessExecutionData.PROCESS_DATA_KEY, removalResult.container());
        return removalResult.removed();
    }

    /**
     * Writes a value into an arbitrary destination-key root. The root may be an object or array. If the root is
     * {@code null}, a matching container root is created.
     *
     * @return The mutated root object or array.
     */
    @Nonnull
    public static Object writeDestinationKeyValue(@Nullable Object root,
                                                  @Nullable String destinationKey,
                                                  @Nullable Object value) {
        var segments = parseDestinationKeySegments(destinationKey, true);
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("Destination key must not be empty when writing a value.");
        }

        assertNoImplicitWildcards(segments, "write");
        var mutableRoot = ensureMutableDestinationRoot(root, segments.getFirst());
        writePathValue(
                mutableRoot,
                segments,
                0,
                value,
                List.of(),
                new int[]{0}
        );
        return mutableRoot;
    }

    /**
     * Writes a value into an arbitrary destination-key root using explicit wildcard indices.
     *
     * @return The mutated root object or array.
     */
    @Nonnull
    public static Object writeDestinationKeyValue(@Nullable Object root,
                                                  @Nullable String destinationKey,
                                                  @Nullable Object value,
                                                  @Nullable List<Integer> wildcardIndices) {
        var segments = parseDestinationKeySegments(destinationKey, true);
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("Destination key must not be empty when writing a value.");
        }

        var validatedWildcardIndices = validateExplicitWildcardIndices(segments, wildcardIndices);
        var mutableRoot = ensureMutableDestinationRoot(root, segments.getFirst());
        writePathValue(
                mutableRoot,
                segments,
                0,
                value,
                validatedWildcardIndices,
                new int[]{0}
        );
        return mutableRoot;
    }

    @Nonnull
    private static List<String> parseDestinationKeySegments(@Nullable String destinationKey,
                                                            boolean allowArrayRoot) {
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

        if (!allowArrayRoot && isArraySegment(segments.getFirst())) {
            throw new IllegalArgumentException("Destination key must start with an object segment.");
        }

        return segments;
    }

    private static void assertNoImplicitWildcards(@Nonnull List<String> segments,
                                                  @Nonnull String operation) {
        if (countWildcardSegments(segments) == 0) {
            return;
        }

        throw new IllegalArgumentException(
                "Wildcard destination keys require explicit indices to " + operation + " a value."
        );
    }

    @Nonnull
    private static List<Integer> validateExplicitWildcardIndices(@Nonnull List<String> segments,
                                                                 @Nullable List<Integer> wildcardIndices) {
        var wildcardCount = countWildcardSegments(segments);
        if (wildcardCount == 0) {
            if (wildcardIndices != null) {
                throw new IllegalArgumentException("Wildcard indices can only be provided for destination keys that contain '*'.");
            }
            return List.of();
        }

        if (wildcardIndices == null) {
            throw new IllegalArgumentException("Wildcard destination keys require explicit indices.");
        }

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
                                       @Nonnull List<Integer> wildcardIndices,
                                       @Nonnull int[] wildcardIndexCursor) {
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
            writePathValue(nextContainer, segments, currentIndex + 1, value, wildcardIndices, wildcardIndexCursor);
            return;
        }

        if (!(container instanceof List<?> currentListContainer)) {
            throw new IllegalStateException("Destination key expects an array segment, but the current value is not a list.");
        }

        var currentList = toMutableList(currentListContainer);
        var arrayIndex = resolveBoundArrayIndex(segment, wildcardIndices, wildcardIndexCursor);
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
        writePathValue(nextContainer, segments, currentIndex + 1, value, wildcardIndices, wildcardIndexCursor);
    }

    @Nonnull
    private static DestinationKeyRemovalResult removePathValue(@Nullable Object container,
                                                               @Nonnull List<String> segments,
                                                               int currentIndex,
                                                               boolean cleanupEmptyContainers,
                                                               @Nullable List<Integer> wildcardIndices,
                                                               @Nonnull int[] wildcardIndexCursor) {
        if (container == null) {
            return DestinationKeyRemovalResult.NOT_FOUND;
        }

        var segment = segments.get(currentIndex);
        var isLastSegment = currentIndex == segments.size() - 1;

        if (container instanceof Map<?, ?> currentMapContainer) {
            var currentMap = toMutableStringObjectMap(currentMapContainer);
            if (!currentMap.containsKey(segment)) {
                return new DestinationKeyRemovalResult(currentMap, false, currentMap.isEmpty());
            }

            if (isLastSegment) {
                currentMap.remove(segment);
                return new DestinationKeyRemovalResult(currentMap, true, currentMap.isEmpty());
            }

            var child = currentMap.get(segment);
            var childResult = removePathValue(
                    child,
                    segments,
                    currentIndex + 1,
                    cleanupEmptyContainers,
                    wildcardIndices,
                    wildcardIndexCursor
            );
            if (!childResult.removed()) {
                return new DestinationKeyRemovalResult(currentMap, false, currentMap.isEmpty());
            }

            if (cleanupEmptyContainers && childResult.containerEmpty()) {
                currentMap.remove(segment);
            } else if (childResult.container() != child) {
                currentMap.put(segment, childResult.container());
            }

            return new DestinationKeyRemovalResult(currentMap, true, currentMap.isEmpty());
        }

        if (!(container instanceof List<?> currentListContainer)) {
            return new DestinationKeyRemovalResult(container, false, false);
        }

        var currentList = toMutableList(currentListContainer);
        if ("*".equals(segment) && wildcardIndices == null) {
            if (isLastSegment) {
                if (currentList.isEmpty()) {
                    return new DestinationKeyRemovalResult(currentList, false, true);
                }

                currentList.clear();
                return new DestinationKeyRemovalResult(currentList, true, true);
            }

            var removedAny = false;
            for (int i = currentList.size() - 1; i >= 0; i--) {
                var child = currentList.get(i);
                var childResult = removePathValue(
                        child,
                        segments,
                        currentIndex + 1,
                        cleanupEmptyContainers,
                        null,
                        wildcardIndexCursor
                );
                if (!childResult.removed()) {
                    continue;
                }

                removedAny = true;
                if (cleanupEmptyContainers && childResult.containerEmpty()) {
                    currentList.remove(i);
                } else if (childResult.container() != child) {
                    currentList.set(i, childResult.container());
                }
            }

            return new DestinationKeyRemovalResult(currentList, removedAny, currentList.isEmpty());
        }

        var arrayIndex = resolveRemovalArrayIndex(segment, wildcardIndices, wildcardIndexCursor);
        if (arrayIndex == null || arrayIndex < 0 || arrayIndex >= currentList.size()) {
            return new DestinationKeyRemovalResult(currentList, false, currentList.isEmpty());
        }

        if (isLastSegment) {
            currentList.remove((int) arrayIndex);
            return new DestinationKeyRemovalResult(currentList, true, currentList.isEmpty());
        }

        var child = currentList.get(arrayIndex);
        var childResult = removePathValue(
                child,
                segments,
                currentIndex + 1,
                cleanupEmptyContainers,
                wildcardIndices,
                wildcardIndexCursor
        );
        if (!childResult.removed()) {
            return new DestinationKeyRemovalResult(currentList, false, currentList.isEmpty());
        }

        if (cleanupEmptyContainers && childResult.containerEmpty()) {
            currentList.remove((int) arrayIndex);
        } else if (childResult.container() != child) {
            currentList.set(arrayIndex, childResult.container());
        }

        return new DestinationKeyRemovalResult(currentList, true, currentList.isEmpty());
    }

    @Nonnull
    private static Object ensureMutableDestinationRoot(@Nullable Object root,
                                                       @Nonnull String firstSegment) {
        if (isArraySegment(firstSegment)) {
            if (root == null) {
                return new ArrayList<>();
            }
            if (root instanceof List<?> currentList) {
                return toMutableList(currentList);
            }
            throw new IllegalStateException("Destination key expects an array root, but the current value is not a list.");
        }

        if (root == null) {
            return new LinkedHashMap<String, Object>();
        }
        if (root instanceof Map<?, ?> currentMap) {
            return toMutableStringObjectMap(currentMap);
        }
        throw new IllegalStateException("Destination key expects an object root, but the current value is not an object.");
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
        return "*".equals(segment) || resolveNumericArrayIndex(segment) != null;
    }

    @Nullable
    private static Integer resolveReadArrayIndex(@Nullable String segment,
                                                 @Nullable List<Integer> wildcardIndices,
                                                 @Nonnull int[] wildcardIndexCursor) {
        if ("*".equals(segment)) {
            return wildcardIndices.get(wildcardIndexCursor[0]++);
        }

        return resolveNumericArrayIndex(segment);
    }

    @Nullable
    private static Integer resolveBoundArrayIndex(@Nullable String segment,
                                                  @Nonnull List<Integer> wildcardIndices,
                                                  @Nonnull int[] wildcardIndexCursor) {
        if ("*".equals(segment)) {
            return wildcardIndices.get(wildcardIndexCursor[0]++);
        }
        return resolveNumericArrayIndex(segment);
    }

    @Nullable
    private static Integer resolveRemovalArrayIndex(@Nullable String segment,
                                                    @Nullable List<Integer> wildcardIndices,
                                                    @Nonnull int[] wildcardIndexCursor) {
        if ("*".equals(segment)) {
            if (wildcardIndices == null) {
                return null;
            }
            return wildcardIndices.get(wildcardIndexCursor[0]++);
        }
        return resolveNumericArrayIndex(segment);
    }

    @Nullable
    private static Integer resolveNumericArrayIndex(@Nullable String segment) {
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

    private static void collectWildcardIndexTuples(@Nullable Object container,
                                                   @Nonnull List<String> segments,
                                                   int currentIndex,
                                                   @Nonnull List<Integer> currentWildcardIndices,
                                                   @Nonnull List<List<Integer>> target) {
        if (currentIndex >= segments.size()) {
            target.add(List.copyOf(currentWildcardIndices));
            return;
        }

        if (container == null) {
            if (countWildcardSegments(segments, currentIndex) == 0) {
                target.add(List.copyOf(currentWildcardIndices));
            }
            return;
        }

        var segment = segments.get(currentIndex);
        if (container instanceof Map<?, ?> currentMap) {
            if (isArraySegment(segment)) {
                if (countWildcardSegments(segments, currentIndex) == 0) {
                    target.add(List.copyOf(currentWildcardIndices));
                }
                return;
            }

            if (!currentMap.containsKey(segment)) {
                if (countWildcardSegments(segments, currentIndex + 1) == 0) {
                    target.add(List.copyOf(currentWildcardIndices));
                }
                return;
            }

            collectWildcardIndexTuples(
                    currentMap.get(segment),
                    segments,
                    currentIndex + 1,
                    currentWildcardIndices,
                    target
            );
            return;
        }

        if (!(container instanceof List<?> currentList)) {
            if (countWildcardSegments(segments, currentIndex) == 0) {
                target.add(List.copyOf(currentWildcardIndices));
            }
            return;
        }

        if ("*".equals(segment)) {
            for (int i = 0; i < currentList.size(); i++) {
                currentWildcardIndices.add(i);
                collectWildcardIndexTuples(
                        currentList.get(i),
                        segments,
                        currentIndex + 1,
                        currentWildcardIndices,
                        target
                );
                currentWildcardIndices.remove(currentWildcardIndices.size() - 1);
            }
            return;
        }

        var arrayIndex = resolveNumericArrayIndex(segment);
        if (arrayIndex == null || arrayIndex < 0 || arrayIndex >= currentList.size()) {
            if (countWildcardSegments(segments, currentIndex + 1) == 0) {
                target.add(List.copyOf(currentWildcardIndices));
            }
            return;
        }

        collectWildcardIndexTuples(
                currentList.get(arrayIndex),
                segments,
                currentIndex + 1,
                currentWildcardIndices,
                target
        );
    }

    private static int countWildcardSegments(@Nonnull List<String> segments) {
        return countWildcardSegments(segments, 0);
    }

    private static int countWildcardSegments(@Nonnull List<String> segments,
                                             int startIndex) {
        var count = 0;
        for (var i = startIndex; i < segments.size(); i++) {
            if ("*".equals(segments.get(i))) {
                count++;
            }
        }
        return count;
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
