package de.aivot.GoverBackend.elements.models;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The inputs for an element structure. The keys are the ids of the elements. The values are the values of the elements. The values can be of any type, depending on the element
 * type. For example, a text element can have a string value, while a number element can have a double value. The values can also be null if the element has no value. The values
 * can also be another AuthoredValues object if the element is a container or an array of AuthoredValues if it's a replicating container.
 */
public class AuthoredElementValues extends HashMap<String, Object> implements Cloneable {
    @Override
    public AuthoredElementValues clone() {
        var clone = (AuthoredElementValues) super.clone();
        for (var entry : entrySet()) {
            clone.put(entry.getKey(), cloneValue(entry.getValue()));
        }
        return clone;
    }

    private static Object cloneValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof AuthoredElementValues authoredElementValues) {
            return authoredElementValues.clone();
        }

        if (value instanceof Map<?, ?> map) {
            var clone = new LinkedHashMap<Object, Object>();
            for (var entry : map.entrySet()) {
                clone.put(cloneValue(entry.getKey()), cloneValue(entry.getValue()));
            }
            return clone;
        }

        if (value instanceof List<?> list) {
            var clone = new ArrayList<>(list.size());
            for (var item : list) {
                clone.add(cloneValue(item));
            }
            return clone;
        }

        if (value instanceof Set<?> set) {
            var clone = new LinkedHashSet<>();
            for (var item : set) {
                clone.add(cloneValue(item));
            }
            return clone;
        }

        if (value instanceof Collection<?> collection) {
            var clone = new ArrayList<>(collection.size());
            for (var item : collection) {
                clone.add(cloneValue(item));
            }
            return clone;
        }

        if (value.getClass().isArray()) {
            return cloneArray(value);
        }

        return value;
    }

    private static Object cloneArray(Object value) {
        var length = Array.getLength(value);
        var componentType = value.getClass().getComponentType();

        if (componentType.isPrimitive()) {
            var clone = Array.newInstance(componentType, length);
            for (var i = 0; i < length; i++) {
                Array.set(clone, i, Array.get(value, i));
            }
            return clone;
        }

        var clonedItems = new Object[length];
        var canPreserveComponentType = true;

        for (var i = 0; i < length; i++) {
            var clonedItem = cloneValue(Array.get(value, i));
            clonedItems[i] = clonedItem;
            if (clonedItem != null && !componentType.isInstance(clonedItem)) {
                canPreserveComponentType = false;
            }
        }

        var clone = Array.newInstance(canPreserveComponentType ? componentType : Object.class, length);
        for (var i = 0; i < length; i++) {
            Array.set(clone, i, clonedItems[i]);
        }
        return clone;
    }
}
