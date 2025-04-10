package de.aivot.GoverBackend.services;

import de.aivot.GoverBackend.models.lib.DiffItem;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;
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

                var childDiffs = createDiff(oldValue, newValue, currentPath + "/" + key);
                diffItems.addAll(childDiffs);
            } else {
                diffItems.add(new DiffItem(currentPath + "/" + key, oldMap.get(key), null));
            }
        }

        for (var key : newMap.keySet()) {
            if (foundKeys.contains(key)) {
                continue;
            }

            diffItems.add(new DiffItem(currentPath + "/" + key, null, newMap.get(key)));
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

            var childDiffs = createDiff(oldChild, newChild, currentPath + "/" + i);
            diffItems.addAll(childDiffs);
        }

        return diffItems;
    }

    public static JSONObject rollBackDiff(JSONObject targetObject, DiffItem diffItem) {
        var path = List.of(diffItem.field().split("/"));
        path = path.subList(1, path.size());
        rollBackDiff(targetObject, diffItem, path);
        return targetObject;
    }

    private static void rollBackDiff(Object targetObject, DiffItem diffToApply, List<String> remainingPath) {
        switch (targetObject) {
            case JSONObject jsonObject:
                if (remainingPath.size() == 1) {
                    jsonObject.put(remainingPath.getFirst(), diffToApply.oldValue());
                } else {
                    Object childObj = jsonObject.get(remainingPath.getFirst());
                    rollBackDiff(childObj, diffToApply, remainingPath.subList(1, remainingPath.size()));
                }
                break;

            case Map<?, ?> _map:
                var map = (Map<String, Object>) _map;

                if (remainingPath.size() == 1) {
                    map.put(remainingPath.getFirst(), diffToApply.oldValue());
                } else {
                    Object childObj = map.get(remainingPath.getFirst());
                    rollBackDiff(childObj, diffToApply, remainingPath.subList(1, remainingPath.size()));
                }
                break;

            case JSONArray jsonArray:
                var jsonArrayIndex = Integer.parseInt(remainingPath.getFirst());
                if (remainingPath.size() == 1) {
                    jsonArray.put(jsonArrayIndex, diffToApply.oldValue());
                } else {
                    var jsonArrayChildToChange = jsonArray.get(jsonArrayIndex);
                    rollBackDiff(jsonArrayChildToChange, diffToApply, remainingPath.subList(1, remainingPath.size()));
                }
                break;

            case List<?> _list:
                var list = (List<Object>) _list;
                var listIndex = Integer.parseInt(remainingPath.getFirst());
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
}
