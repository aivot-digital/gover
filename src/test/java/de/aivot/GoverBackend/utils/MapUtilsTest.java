package de.aivot.GoverBackend.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapUtilsTest {
    @Test
    void deepCopyShouldCopyNestedMapsAndCollections() {
        var nestedMap = new LinkedHashMap<String, Object>();
        nestedMap.put("name", "Ada");

        var listItem = new LinkedHashMap<String, Object>();
        listItem.put("score", 7);
        var list = new ArrayList<Object>();
        list.add(listItem);

        var setItem = new LinkedHashMap<String, Object>();
        setItem.put("role", "admin");
        var set = new LinkedHashSet<Object>();
        set.add(setItem);

        var arrayItem = new LinkedHashMap<String, Object>();
        arrayItem.put("city", "London");
        var array = new Object[]{arrayItem};
        var bytes = new byte[]{1, 2, 3};

        var source = new LinkedHashMap<String, Object>();
        source.put("person", nestedMap);
        source.put("items", list);
        source.put("roles", set);
        source.put("places", array);
        source.put("bytes", bytes);
        source.put("unchangedScalar", "value");

        var copy = MapUtils.deepCopy(source);

        assertNotSame(source, copy);
        assertEquals("value", copy.get("unchangedScalar"));

        @SuppressWarnings("unchecked")
        var copiedNestedMap = (Map<String, Object>) copy.get("person");
        assertNotSame(nestedMap, copiedNestedMap);
        copiedNestedMap.put("name", "Grace");
        assertEquals("Ada", nestedMap.get("name"));

        @SuppressWarnings("unchecked")
        var copiedList = (List<Object>) copy.get("items");
        assertNotSame(list, copiedList);
        @SuppressWarnings("unchecked")
        var copiedListItem = (Map<String, Object>) copiedList.get(0);
        assertNotSame(listItem, copiedListItem);
        copiedListItem.put("score", 9);
        assertEquals(7, listItem.get("score"));

        @SuppressWarnings("unchecked")
        var copiedSet = (Set<Object>) copy.get("roles");
        assertNotSame(set, copiedSet);
        @SuppressWarnings("unchecked")
        var copiedSetItem = (Map<String, Object>) copiedSet.iterator().next();
        assertNotSame(setItem, copiedSetItem);
        copiedSetItem.put("role", "owner");
        assertEquals("admin", setItem.get("role"));

        var copiedArray = (Object[]) copy.get("places");
        assertNotSame(array, copiedArray);
        @SuppressWarnings("unchecked")
        var copiedArrayItem = (Map<String, Object>) copiedArray[0];
        assertNotSame(arrayItem, copiedArrayItem);
        copiedArrayItem.put("city", "Paris");
        assertEquals("London", arrayItem.get("city"));

        var copiedBytes = (byte[]) copy.get("bytes");
        assertNotSame(bytes, copiedBytes);
        assertArrayEquals(bytes, copiedBytes);
    }

    @Test
    void deepCopyShouldPreserveMapKeys() {
        var source = new LinkedHashMap<Object, Object>();
        source.put("kept", "value");
        source.put(1, "also-kept");

        var copy = MapUtils.deepCopy(source);

        assertEquals("value", copy.get("kept"));
        assertEquals("also-kept", copy.get(1));
    }

    @Test
    void deepCopyShouldReturnEmptyMapForNullInput() {
        var copy = MapUtils.deepCopy(null);

        assertTrue(copy.isEmpty());
    }
}
