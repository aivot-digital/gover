package de.aivot.prosuna.backend.elements.models;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class AuthoredElementValuesTest {
    @Test
    void cloneCreatesDeepCopyForNestedContainers() {
        var nestedValues = new AuthoredElementValues();
        nestedValues.put("name", "original");

        var listItem = new AuthoredElementValues();
        listItem.put("value", "list-original");

        var mapValue = new LinkedHashMap<String, Object>();
        mapValue.put("nested", nestedValues);

        var setValue = new LinkedHashSet<Object>();
        setValue.add(listItem);

        var values = new AuthoredElementValues();
        values.put("nested", nestedValues);
        values.put("list", new ArrayList<>(List.of(listItem)));
        values.put("map", mapValue);
        values.put("set", setValue);

        var clone = values.clone();

        assertEquals(values, clone);
        assertNotSame(values, clone);
        assertNotSame(values.get("nested"), clone.get("nested"));
        assertNotSame(values.get("list"), clone.get("list"));
        assertNotSame(values.get("map"), clone.get("map"));
        assertNotSame(values.get("set"), clone.get("set"));

        ((AuthoredElementValues) clone.get("nested")).put("name", "changed");
        ((AuthoredElementValues) ((List<?>) clone.get("list")).getFirst()).put("value", "list-changed");
        ((AuthoredElementValues) ((Map<?, ?>) clone.get("map")).get("nested")).put("name", "map-changed");

        assertEquals("original", nestedValues.get("name"));
        assertEquals("list-original", listItem.get("value"));
    }

    @Test
    void cloneCreatesDeepCopyForArrays() {
        var nestedValues = new AuthoredElementValues();
        nestedValues.put("name", "original");

        var values = new AuthoredElementValues();
        values.put("objects", new Object[]{nestedValues});
        values.put("numbers", new int[]{1, 2, 3});

        var clone = values.clone();

        assertArrayEquals((Object[]) values.get("objects"), (Object[]) clone.get("objects"));
        assertArrayEquals((int[]) values.get("numbers"), (int[]) clone.get("numbers"));
        assertNotSame(values.get("objects"), clone.get("objects"));
        assertNotSame(values.get("numbers"), clone.get("numbers"));
        assertNotSame(((Object[]) values.get("objects"))[0], ((Object[]) clone.get("objects"))[0]);

        ((AuthoredElementValues) ((Object[]) clone.get("objects"))[0]).put("name", "changed");

        assertEquals("original", nestedValues.get("name"));
    }
}
