package de.aivot.GoverBackend.models.elements;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public abstract class AbstractElementTest<T extends BaseElement> {
    protected abstract Map<String, Object> getJSON();

    protected abstract T newItem(Map<String, Object> json);

    protected abstract void testAllFieldsFilled(T item);

    protected abstract void testAllFieldsNull(T item);

    protected T getItem() {
        return newItem(getJSON());
    }
    @Test
    void testConstructor() {
        var item = getItem();
        testAllFieldsFilled(item);
    }

    @Test
    void testConstructorNoData() {
        var item = newItem(new HashMap<>());
        testAllFieldsNull(item);
    }

    @Test
    void testConstructorMissingData() {
        Set<String> keys = getJSON().keySet();

        for (String key : keys) {
            var jsonCopy = new HashMap<>(getJSON());
            jsonCopy.remove(key);
            assertDoesNotThrow(() -> {
                newItem(jsonCopy);
            });
        }
    }

    @Test
    void testConstructorWronglyTypedData() {
        Set<String> keys = getJSON().keySet();

        for (String key : keys) {
            var jsonCopy = new HashMap<>(getJSON());
            jsonCopy.put(key, new Object());
            assertDoesNotThrow(() -> {
                newItem(jsonCopy);
            });
        }
    }
}
