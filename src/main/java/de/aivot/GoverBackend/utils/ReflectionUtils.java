package de.aivot.GoverBackend.utils;

import jakarta.annotation.Nonnull;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ReflectionUtils {
    public static List<Field> getAllDeclaredFields(Class<?> type) {
        var allFields = Arrays.asList(type.getDeclaredFields());

        var superclass = type.getSuperclass();

        if (superclass != null) {
            allFields.addAll(getAllDeclaredFields(superclass));
        }

        return allFields;
    }

    public static Optional<Field> getDeclaredField(Class<?> type, String fieldName) {
        try {
            return Optional.of(type.getDeclaredField(fieldName));
        } catch (NoSuchFieldException e) {
            var superclass = type.getSuperclass();

            if (superclass != null) {
                return getDeclaredField(superclass, fieldName);
            } else {
                return Optional.empty();
            }
        }
    }

    public static Optional<Class<?>> getGenericType(@Nonnull Field field, int index) {
        if (field.getGenericType() instanceof ParameterizedType genericType) {
            if (genericType.getActualTypeArguments().length <= index) {
                return Optional.empty();
            }
            var actualTypeArgument = genericType.getActualTypeArguments()[index];
            if (actualTypeArgument instanceof Class<?> actualClass) {
                return Optional.of(actualClass);
            }
        } else {
            return Optional.empty();
        }

        return Optional.empty();
    }
}
