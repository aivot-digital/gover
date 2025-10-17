package de.aivot.GoverBackend.xdf.v2.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to count occurrences of IDs and generate unique IDs by appending a counter suffix.
 * For example, if "item" is added three times, the generated IDs will be "item", "item_2", and "item_3".
 */
public class XdfIdCounter {
    private final Map<String, Integer> counter = new HashMap<>();

    /**
     * Counts the occurrences of the given ID and returns a unique ID.
     * If the ID has been seen before, appends a suffix with the count.
     * @param id the original ID
     * @return a unique ID based on the count of occurrences
     */
    public String countId(String id) {
        if (counter.containsKey(id)) {
            int count = counter.get(id) + 1;
            counter.put(id, count);
            return id + "_" + count;
        } else {
            counter.put(id, 1);
            return id;
        }
    }
}
