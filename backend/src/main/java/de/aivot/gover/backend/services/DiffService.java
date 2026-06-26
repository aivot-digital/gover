package de.aivot.gover.backend.services;

import de.aivot.gover.backend.models.lib.DiffItem;
import org.json.JSONArray;
import org.json.JSONObject;

import jakarta.annotation.Nullable;
import java.util.*;

public class DiffService {
    public static List<DiffItem> createDiff(JSONObject oldObj, JSONObject newObj) {
        return createDiff(oldObj, newObj, "");
    }

    private static List<DiffItem> createDiff(@Nullable Object oldObject, @Nullable Object newObject, String currentPath) {
        if (oldObject == null && newObject == null) {
            return Collections.emptyList();
        }

        if (oldObject == null) {
            return List.of(new DiffItem(currentPath, null, newObject));
        }

        if (newObject == null) {
            return List.of(new DiffItem(currentPath, oldObject, null));
        }

        // Check if the objects are of the same type. If not, create a diff.
        if (oldObject.getClass() != newObject.getClass()) {
            return List.of(new DiffItem(currentPath, oldObject, newObject));
        }

        if (oldObject instanceof JSONObject oldJobject && newObject instanceof JSONObject newJObject && !Objects.equals(oldJobject.get("id"), newJObject.get("id"))) {
            return List.of(new DiffItem(currentPath, oldObject, newObject));
        }

        if (oldObject instanceof Map<?, ?> oldJobject && newObject instanceof Map<?, ?> newJObject && !Objects.equals(oldJobject.get("id"), newJObject.get("id"))) {
            return List.of(new DiffItem(currentPath, oldObject, newObject));
        }

        // Match over the type of the old object and derive diffs based on the main types.
        return switch (oldObject) {
            case JSONObject oldJsonObject -> {
                var newJsonObject = (JSONObject) newObject;
                yield getDiffItemsForMaps(currentPath, oldJsonObject.toMap(), newJsonObject.toMap());
            }
            case Map<?, ?> oldMap -> {
                var newMap = (Map<?, ?>) newObject;
                yield getDiffItemsForMaps(currentPath, oldMap, newMap);
            }
            case JSONArray oldJsonArray -> {
                var newJsonArray = (JSONArray) newObject;
                yield getDiffItemsForLists(currentPath, oldJsonArray.toList(), newJsonArray.toList());
            }
            case List<?> oldList -> {
                var newList = (List<?>) newObject;
                yield getDiffItemsForLists(currentPath, oldList, newList);
            }
            default -> {
                if (Objects.equals(oldObject, newObject)) {
                    yield Collections.emptyList();
                } else {
                    yield List.of(new DiffItem(currentPath, oldObject, newObject));
                }
            }
        };
    }

    private static List<DiffItem> getDiffItemsForMaps(String currentPath, Map<?, ?> oldMap, Map<?, ?> newMap) {
        var diffItems = new LinkedList<DiffItem>();

        var foundKeys = new HashSet<>();

        for (var key : oldMap.keySet()) {
            foundKeys.add(key);

            if (newMap.containsKey(key)) {
                var oldValue = oldMap.get(key);
                var newValue = newMap.get(key);

                var childDiffs = createDiff(oldValue, newValue, appendMapPath(currentPath, key));
                diffItems.addAll(childDiffs);
            } else {
                diffItems.add(new DiffItem(appendMapPath(currentPath, key), oldMap.get(key), null));
            }
        }

        for (var key : newMap.keySet()) {
            if (foundKeys.contains(key)) {
                continue;
            }

            diffItems.add(new DiffItem(appendMapPath(currentPath, key), null, newMap.get(key)));
        }

        return diffItems;
    }

    private static List<DiffItem> getDiffItemsForLists(String currentPath, List<?> oldList, List<?> newList) {
        // Compare length of the lists. If they are not equal, create a diff.
        if (oldList.size() != newList.size()) {
            return List.of(new DiffItem(currentPath, oldList, newList));
        }

        List<DiffItem> diffItems = new LinkedList<>();

        // Iterate over the list and compare each element.
        for (int i = 0; i < oldList.size(); i++) {
            var oldChild = oldList.get(i);
            var newChild = newList.get(i);

            var childDiffs = createDiff(oldChild, newChild, appendListPath(currentPath, i));
            diffItems.addAll(childDiffs);
        }

        return diffItems;
    }

    public static JSONObject rollBackDiff(JSONObject targetObject, DiffItem diffItem) {
        var path = parsePath(diffItem.field());
        if (path.isEmpty()) {
            applyRootDiff(targetObject, diffItem.oldValue());
            return targetObject;
        }

        rollBackDiff(targetObject, diffItem, path);
        return targetObject;
    }

    private static void rollBackDiff(Object targetObject, DiffItem diffToApply, List<PathSegment> remainingPath) {
        var currentSegment = remainingPath.getFirst();

        switch (targetObject) {
            case JSONObject jsonObject:
                var jsonObjectKey = currentSegment.asKey();
                if (remainingPath.size() == 1) {
                    jsonObject.put(jsonObjectKey, diffToApply.oldValue());
                } else {
                    Object childObj = jsonObject.get(jsonObjectKey);
                    rollBackDiff(childObj, diffToApply, remainingPath.subList(1, remainingPath.size()));
                }
                break;

            case Map<?, ?> _map:
                var map = (Map<String, Object>) _map;
                var mapKey = currentSegment.asKey();

                if (remainingPath.size() == 1) {
                    map.put(mapKey, diffToApply.oldValue());
                } else {
                    Object childObj = map.get(mapKey);
                    rollBackDiff(childObj, diffToApply, remainingPath.subList(1, remainingPath.size()));
                }
                break;

            case JSONArray jsonArray:
                var jsonArrayIndex = currentSegment.asIndex();
                if (remainingPath.size() == 1) {
                    jsonArray.put(jsonArrayIndex, diffToApply.oldValue());
                } else {
                    var jsonArrayChildToChange = jsonArray.get(jsonArrayIndex);
                    rollBackDiff(jsonArrayChildToChange, diffToApply, remainingPath.subList(1, remainingPath.size()));
                }
                break;

            case List<?> _list:
                var list = (List<Object>) _list;
                var listIndex = currentSegment.asIndex();
                if (remainingPath.size() == 1) {
                    list.set(listIndex, diffToApply.oldValue());
                } else {
                    var listChildToChange = list.get(listIndex);
                    rollBackDiff(listChildToChange, diffToApply, remainingPath.subList(1, remainingPath.size()));
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown object type. Expected JSONObject, JSONArray, List or Map but got " + targetObject.getClass().getName());
        }
    }

    private static String appendMapPath(String currentPath, Object key) {
        var pathSegment = key.toString();
        if (currentPath.isEmpty()) {
            return pathSegment;
        }

        return currentPath + "." + pathSegment;
    }

    private static String appendListPath(String currentPath, int index) {
        return currentPath + "[" + index + "]";
    }

    private static List<PathSegment> parsePath(String field) {
        if (field == null || field.isBlank() || "/".equals(field)) {
            return Collections.emptyList();
        }

        if (field.contains("/")) {
            return Arrays.stream(field.split("/"))
                    .filter(segment -> !segment.isBlank())
                    .map(PathSegment::key)
                    .toList();
        }

        var pathSegments = new ArrayList<PathSegment>();
        var currentToken = new StringBuilder();

        for (int i = 0; i < field.length(); i++) {
            var currentChar = field.charAt(i);
            switch (currentChar) {
                case '.' -> flushCurrentToken(pathSegments, currentToken);
                case '[' -> {
                    flushCurrentToken(pathSegments, currentToken);

                    var closingBracketIndex = field.indexOf(']', i);
                    if (closingBracketIndex < 0) {
                        throw new IllegalArgumentException("Invalid diff path: " + field);
                    }

                    var indexToken = field.substring(i + 1, closingBracketIndex);
                    try {
                        pathSegments.add(PathSegment.index(Integer.parseInt(indexToken)));
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("Invalid diff path: " + field, ex);
                    }

                    i = closingBracketIndex;
                }
                default -> currentToken.append(currentChar);
            }
        }

        flushCurrentToken(pathSegments, currentToken);

        return pathSegments;
    }

    private static void flushCurrentToken(List<PathSegment> pathSegments, StringBuilder currentToken) {
        if (currentToken.isEmpty()) {
            return;
        }

        pathSegments.add(PathSegment.key(currentToken.toString()));
        currentToken.setLength(0);
    }

    private static void applyRootDiff(JSONObject targetObject, @Nullable Object oldValue) {
        var existingKeys = new ArrayList<>(targetObject.keySet());
        for (var existingKey : existingKeys) {
            targetObject.remove(existingKey);
        }

        if (oldValue == null) {
            return;
        }

        if (oldValue instanceof JSONObject oldJsonObject) {
            for (var key : oldJsonObject.keySet()) {
                targetObject.put(key, oldJsonObject.get(key));
            }
            return;
        }

        if (oldValue instanceof Map<?, ?> oldMap) {
            for (var entry : oldMap.entrySet()) {
                targetObject.put(entry.getKey().toString(), entry.getValue());
            }
            return;
        }

        throw new IllegalArgumentException("Cannot apply root diff with value type " + oldValue.getClass().getName());
    }

    private record PathSegment(@Nullable String key, @Nullable Integer index) {
        private static PathSegment key(String key) {
            return new PathSegment(key, null);
        }

        private static PathSegment index(int index) {
            return new PathSegment(null, index);
        }

        private String asKey() {
            if (key == null) {
                throw new IllegalArgumentException("Expected object key path segment but got array index");
            }

            return key;
        }

        private int asIndex() {
            if (index != null) {
                return index;
            }

            if (key == null) {
                throw new IllegalArgumentException("Missing path segment");
            }

            return Integer.parseInt(key);
        }
    }
}
