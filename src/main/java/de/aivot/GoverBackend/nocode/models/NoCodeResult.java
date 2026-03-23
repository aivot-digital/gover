package de.aivot.GoverBackend.nocode.models;

import org.json.JSONObject;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;

/**
 * Represents the result of a NoCode evaluation.
 * Contains the value of the result.
 */
public class NoCodeResult {
    private final Object value;

    public NoCodeResult(@Nullable Object value) {
        this.value = value;
    }

    /**
     * Returns whether the result is null.
     *
     * @return whether the result is null
     */
    @Nonnull
    public Boolean isNull() {
        return value == null;
    }

    /**
     * Returns the value of the result as an object.
     *
     * @return the value of the result as an object.
     */
    @Nullable
    public Object getValue() {
        return value;
    }

    /**
     * Returns the value of the result as a boolean.
     * If the value is null, false is returned.
     *
     * @return the value of the result as a boolean.
     */
    @Nonnull
    public Boolean getValueAsBoolean() {
        if (isNull() || value == null) {
            return false;
        }

        return switch (value) {
            case String str -> Boolean.parseBoolean(str);
            case Integer integer -> integer != 0;
            case Float f -> f != 0.0;
            case Double d -> d != 0.0;
            case Long l -> l != 0;
            case Short s -> s != 0;
            case Byte b -> b != 0;
            case Boolean bool -> bool;
            case Collection<?> collection -> !collection.isEmpty();
            case Object o -> !new JSONObject(o).isEmpty();
        };
    }
}
