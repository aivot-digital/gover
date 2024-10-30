package de.aivot.GoverBackend.utils;

import de.aivot.GoverBackend.lib.Identifiable;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class MapUtils {
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

    public static <T, E extends Identifiable<T>> E getEnum(Map<String, Object> map, String key, Class<T> cls, E[] enumValues) {
        return getEnum(map, key, cls, enumValues, null);
    }
    public static <T, E extends Identifiable<T>> E getEnum(Map<String, Object> map, String key, Class<T> cls, E[] enumValues, E def) {
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
