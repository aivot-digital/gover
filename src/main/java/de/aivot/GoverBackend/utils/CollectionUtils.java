package de.aivot.GoverBackend.utils;

import jakarta.annotation.Nullable;
import java.util.Collection;

public class CollectionUtils {
    public static boolean isEmpty(@Nullable Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmpty(@Nullable Collection<?> collection) {
        return !isEmpty(collection);
    }
}
