package de.aivot.prosuna.backend.utils;

import de.aivot.prosuna.backend.lib.models.Identifiable;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public class MapUtils {
    @Nonnull
    public static <K> Map<K, Object> deepCopy(@Nullable Map<K, ?> source) {
        var result = new LinkedHashMap<K, Object>();
        if (source == null) {
            return result;
        }

        for (var entry : source.entrySet()) {
            result.put(entry.getKey(), deepCopyValue(entry.getValue()));
        }

        return result;
    }

    @Nullable
    public static Object deepCopyValue(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Map<?, ?> map) {
            return deepCopy(map);
        }

        if (value instanceof List<?> list) {
            var result = new ArrayList<>(list.size());
            for (var item : list) {
                result.add(deepCopyValue(item));
            }
            return result;
        }

        if (value instanceof Set<?> set) {
            var result = new LinkedHashSet<>();
            for (var item : set) {
                result.add(deepCopyValue(item));
            }
            return result;
        }

        if (value instanceof Collection<?> collection) {
            var result = new ArrayList<>(collection.size());
            for (var item : collection) {
                result.add(deepCopyValue(item));
            }
            return result;
        }

        if (value.getClass().isArray()) {
            return deepCopyArray(value);
        }

        return value;
    }

    @Nonnull
    private static Object deepCopyArray(@Nonnull Object array) {
        var length = Array.getLength(array);
        var componentType = array.getClass().getComponentType();

        if (componentType.isPrimitive()) {
            var result = Array.newInstance(componentType, length);
            System.arraycopy(array, 0, result, 0, length);
            return result;
        }

        var copiedItems = new Object[length];
        var canPreserveComponentType = true;
        for (var i = 0; i < length; i++) {
            var copiedItem = deepCopyValue(Array.get(array, i));
            copiedItems[i] = copiedItem;
            if (copiedItem != null && !componentType.isInstance(copiedItem)) {
                canPreserveComponentType = false;
            }
        }

        var result = Array.newInstance(canPreserveComponentType ? componentType : Object.class, length);
        for (var i = 0; i < length; i++) {
            Array.set(result, i, copiedItems[i]);
        }
        return result;
    }

    @Nullable
    public static <T> T get(Map<String, Object> map, String key, Class<T> cls) {
        return get(map, key, cls, null);
    }

    public static <T> T get(Map<String, Object> map, String key, Class<T> cls, T def) {
        if (map == null) {
            return def;
        }
        Object obj = map.get(key);
        return cls.isInstance(obj) ? (T) obj : def;
    }

    public static <T, R> R getApply(Map<String, Object> map, String key, Class<T> cls, Function<T, R> func) {
        var data = get(map, key, cls);
        return data != null ? func.apply(data) : null;
    }

    public static <T, E extends Identifiable<T>> E getEnum(Map<String, Object> map, String key, Class<T> cls, Class<E> eCls, E[] enumValues) {
        return getEnum(map, key, cls, eCls, enumValues, null);
    }

    public static <T, E extends Identifiable<T>> E getEnum(Map<String, Object> map, String key, Class<T> cls, Class<E> eCls, E[] enumValues, E def) {
        var res = map.get(key);

        if (eCls.isInstance(res)) {
            return (E) res;
        }

        var data = get(map, key, cls);
        return Arrays
                .stream(enumValues)
                .filter(e -> e.matches(data))
                .findFirst()
                .orElse(def);
    }

    public static <T> Collection<T> getCollection(Map<String, Object> map, String key, Function<Map<String, Object>, T> con) {
        Collection<Map<String, Object>> items = get(map, key, Collection.class);
        if (items != null) {
            return items
                    .stream()
                    .filter(Objects::nonNull)
                    .map(con)
                    .toList();
        } else {
            return null;
        }
    }

    public static <T> Collection<T> getCollectionKeepNull(Map<String, Object> map, String key, Function<Map<String, Object>, T> con) {
        Collection<Map<String, Object>> items = get(map, key, Collection.class);
        if (items != null) {
            return items
                    .stream()
                    .map(con)
                    .toList();
        } else {
            return null;
        }
    }

    public static Collection<String> getStringCollection(Map<String, Object> map, String key) {
        Collection<?> items = get(map, key, Collection.class);
        if (items != null) {
            return items
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(i -> i instanceof String)
                    .map(i -> (String) i)
                    .toList();
        } else {
            return null;
        }
    }

    public static Collection<Object> getObjectCollection(Map<String, Object> map, String key) {
        Collection<?> items = get(map, key, Collection.class);
        if (items != null) {
            return items
                    .stream()
                    .filter(Objects::nonNull)
                    .map(i -> (Object) i)
                    .toList();
        } else {
            return null;
        }
    }

    public static String getString(Map<String, Object> map, String key) {
        return get(map, key, String.class);
    }

    public static String getString(Map<String, Object> map, String key, String def) {
        return get(map, key, String.class, def);
    }

    public static Boolean getBoolean(Map<String, Object> map, String key) {
        return get(map, key, Boolean.class);
    }

    public static Boolean getBoolean(Map<String, Object> map, String key, Boolean def) {
        return get(map, key, Boolean.class, def);
    }

    public static Integer getInteger(Map<String, Object> map, String key) {
        return get(map, key, Integer.class);
    }

    public static Integer getInteger(Map<String, Object> map, String key, Integer def) {
        return get(map, key, Integer.class, def);
    }

    public static Double getDouble(Map<String, Object> map, String key) {
        var res = map.get(key);

        return switch (res) {
            case Integer iRes -> iRes.doubleValue();
            case Float fRes -> fRes.doubleValue();
            case String sRes -> Double.parseDouble(sRes);
            case BigDecimal bdRes -> bdRes.doubleValue();
            case null, default -> get(map, key, Double.class);
        };
    }

    public static Double getDouble(Map<String, Object> map, String key, Double def) {
        var res = map.get(key);

        return switch (res) {
            case Integer iRes -> iRes.doubleValue();
            case Float fRes -> fRes.doubleValue();
            case String sRes -> Double.parseDouble(sRes);
            case BigDecimal bdRes -> bdRes.doubleValue();
            case null, default -> get(map, key, Double.class, def);
        };
    }
}
