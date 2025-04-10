package de.aivot.GoverBackend.nocode.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.GoverBackend.lib.models.Identifiable;

/**
 * Enum to represent the data types that can be used in the NoCode system.
 * Each data type has a key that is used to serialize the enum to JSON.
 */
public enum NoCodeDataType implements Identifiable<Integer> {
    Any(0),

    Boolean(10),
    Number(11),
    String(12),
    Date(13),

    List(100),
    Object(101);

    private final Integer key;

    NoCodeDataType(Integer key) {
        this.key = key;
    }

    /**
     * Get the key of the enum.
     * This is used to serialize the enum to JSON.
     *
     * @return The key of the enum.
     */
    @Override
    @JsonValue
    public Integer getKey() {
        return key;
    }

    /**
     * Check if the enum matches another object.
     *
     * @param other The object to compare to.
     * @return True if the object is a NoCodeDataType with the same key.
     */
    @Override
    public boolean matches(Object other) {
        return other instanceof NoCodeDataType nc && nc.key.equals(key);
    }
}
