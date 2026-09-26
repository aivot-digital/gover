package de.aivot.prosuna.backend.utils;

import de.aivot.prosuna.backend.utils.MapUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayDeque;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapUtilsTest {
    @Test
    void deepCopyAppliesSpecialHandlerAtEveryContainerDepth() {
        var leaf = new StringBuilder("original");
        var source = Map.of("nested", List.of(
                Set.of(leaf), new ArrayDeque<>(List.of(leaf)), new StringBuilder[]{leaf}));
        var copy = (Map<?, ?>) MapUtils.deepCopyValue(source,
                value -> value instanceof StringBuilder text ? new StringBuilder(text) : value);
        var items = (List<?>) copy.get("nested");
        var copiedSetLeaf = (StringBuilder) ((Set<?>) items.get(0)).iterator().next();
        var copiedQueueLeaf = (StringBuilder) ((List<?>) items.get(1)).getFirst();
        var copiedArrayLeaf = ((StringBuilder[]) items.get(2))[0];
        copiedSetLeaf.append("-set");
        copiedQueueLeaf.append("-queue");
        copiedArrayLeaf.append("-array");
        assertEquals("original", leaf.toString());
        assertEquals("original-set", copiedSetLeaf.toString());
        assertEquals("original-queue", copiedQueueLeaf.toString());
        assertEquals("original-array", copiedArrayLeaf.toString());
    }

    @Test
    void deepCopyWidensArraysWhenSpecialCopiesHaveAnotherType() {
        var original = new StringBuilder[]{new StringBuilder("value")};
        var copy = (Object[]) MapUtils.deepCopyValue(original,
                value -> value instanceof StringBuilder text ? text.toString() : value);
        assertEquals(Object[].class, copy.getClass());
        assertArrayEquals(new Object[]{"value"}, copy);
        assertEquals(StringBuilder[].class, original.getClass());
    }

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
