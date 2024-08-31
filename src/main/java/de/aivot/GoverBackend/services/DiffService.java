package de.aivot.GoverBackend.services;

import de.aivot.GoverBackend.models.lib.DiffItem;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class DiffService {
    public static List<DiffItem> createDiff(JSONObject oldObj, JSONObject newObj) {
        return createDiff(oldObj, newObj, "");
    }

    private static List<DiffItem> createDiff(JSONObject oldObj, JSONObject newObj, String path) {
        List<DiffItem> diffItems = new LinkedList<>();

        Set<String> foundKeys = new HashSet<>();
        for (String key : oldObj.keySet()) {
            foundKeys.add(key);

            if (newObj.has(key)) {
                var oldValue = oldObj.get(key);
                var newValue = newObj.get(key);

                if (oldValue instanceof JSONArray oldArrayValue && newValue instanceof JSONArray newArrayValue) {
                    diffItems.addAll(createDiff(oldArrayValue, newArrayValue, path + "/" + key));
                } else if (oldValue instanceof JSONObject oldObjectValue && newValue instanceof JSONObject newObjectValue) {
                    diffItems.addAll(createDiff(oldObjectValue, newObjectValue, path + "/" + key));
                } else {
                    if (!oldValue.equals(newValue)) {
                        diffItems.add(new DiffItem(path + "/" + key, oldValue, newValue));
                    }
                }
            } else {
                diffItems.add(new DiffItem(path + "/" + key, oldObj.get(key), null));
            }
        }

        for (String key : newObj.keySet()) {
            if (foundKeys.contains(key)) {
                continue;
            }
            diffItems.add(new DiffItem(path + "/" + key, null, newObj.get(key)));
        }

        return diffItems;
    }

    private static List<DiffItem> createDiff(JSONArray oldArray, JSONArray newArray, String path) {
        List<DiffItem> diffItems = new LinkedList<>();

        if (oldArray.length() != newArray.length()) {
            diffItems.add(new DiffItem(path, oldArray.toList(), newArray.toList()));
        } else {
            for (int i = 0; i < oldArray.length(); i++) {
                var oldItem = oldArray.get(i);
                var newItem = newArray.get(i);

                if (oldItem instanceof JSONObject oldObjectItem && newItem instanceof JSONObject newObjectItem) {
                    diffItems.addAll(createDiff(oldObjectItem, newObjectItem, path + "/" + i));
                } else if (oldItem instanceof JSONArray oldArrayItem && newItem instanceof JSONArray newArrayItem) {
                    diffItems.addAll(createDiff(oldArrayItem, newArrayItem, path + "/" + i));
                } else {
                    if (!oldItem.equals(newItem)) {
                        diffItems.add(new DiffItem(path + "/" + i, oldItem, newItem));
                    }
                }
            }
        }

        return diffItems;
    }
}
