package de.aivot.prosuna.backend.elements.models;

import de.aivot.prosuna.backend.elements.models.input.AuthoredInputValue;
import de.aivot.prosuna.backend.elements.models.input.LiteralAuthoredInputValue;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Values persisted or transported for an authored element structure. Every entry has an explicit input-mode envelope;
 * only {@link EffectiveElementValues} contains the resolved domain values used at runtime.
 */
public class AuthoredElementValues extends HashMap<String, AuthoredInputValue> implements Cloneable {
    public static final String LITERAL_VALUE_PROPERTY = "value";

    /**
     * Wraps one map level. Replicating-container rows require the element-tree-aware conversion in
     * {@code AuthoredInputValueService} when their nested values are effective values.
     */
    public static AuthoredElementValues fromLiteralValues(Map<String, ?> values) {
        var authoredValues = new AuthoredElementValues();
        values.forEach(authoredValues::putLiteral);
        return authoredValues;
    }

    /**
     * Maps a domain path to the persisted payload of its top-level literal input-mode envelope.
     */
    public static List<String> literalValueJsonPath(String... domainPath) {
        if (domainPath.length == 0) {
            throw new IllegalArgumentException("A literal authored value JSON path requires an element id.");
        }

        var jsonPath = new ArrayList<String>(domainPath.length + 1);
        jsonPath.add(domainPath[0]);
        jsonPath.add(LITERAL_VALUE_PROPERTY);
        jsonPath.addAll(List.of(domainPath).subList(1, domainPath.length));
        return List.copyOf(jsonPath);
    }

    public AuthoredElementValues putLiteral(String key, Object value) {
        put(key, new LiteralAuthoredInputValue(value));
        return this;
    }

    @Override
    public AuthoredInputValue put(String key, AuthoredInputValue value) {
        return super.put(
                Objects.requireNonNull(key, "An authored element value requires an element id."),
                Objects.requireNonNull(value, "An authored element value must use an input-mode envelope.")
        );
    }

    @Override
    public void putAll(Map<? extends String, ? extends AuthoredInputValue> values) {
        values.forEach(this::put);
    }

    public Object getLiteral(String key) {
        var value = get(key);
        return value instanceof LiteralAuthoredInputValue literal ? literal.value() : null;
    }

    /**
     * Unwraps one map level and rejects dynamic values. Nested authored rows intentionally remain structured.
     */
    public Map<String, Object> toLiteralValues() {
        var literalValues = new LinkedHashMap<String, Object>();
        for (var entry : entrySet()) {
            if (!(entry.getValue() instanceof LiteralAuthoredInputValue literal)) {
                throw new IllegalStateException("Only literal authored values can be converted without derivation.");
            }
            literalValues.put(entry.getKey(), literal.value());
        }
        return literalValues;
    }

    @Override
    public AuthoredElementValues clone() {
        var clone = (AuthoredElementValues) super.clone();
        for (var entry : entrySet()) {
            clone.put(entry.getKey(), cloneValue(entry.getValue()));
        }
        return clone;
    }

    private static AuthoredInputValue cloneValue(AuthoredInputValue value) {
        if (value instanceof LiteralAuthoredInputValue literal) {
            return new LiteralAuthoredInputValue(cloneLiteralValue(literal.value()));
        }

        return value;
    }

    private static Object cloneLiteralValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof AuthoredElementValues authoredElementValues) {
            return authoredElementValues.clone();
        }

        if (value instanceof Map<?, ?> map) {
            var clone = new LinkedHashMap<Object, Object>();
            for (var entry : map.entrySet()) {
                clone.put(cloneLiteralValue(entry.getKey()), cloneLiteralValue(entry.getValue()));
            }
            return clone;
        }

        if (value instanceof List<?> list) {
            var clone = new ArrayList<>(list.size());
            for (var item : list) {
                clone.add(cloneLiteralValue(item));
            }
            return clone;
        }

        if (value instanceof Set<?> set) {
            var clone = new LinkedHashSet<>();
            for (var item : set) {
                clone.add(cloneLiteralValue(item));
            }
            return clone;
        }

        if (value instanceof Collection<?> collection) {
            var clone = new ArrayList<>(collection.size());
            for (var item : collection) {
                clone.add(cloneLiteralValue(item));
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
            var clonedItem = cloneLiteralValue(Array.get(value, i));
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
