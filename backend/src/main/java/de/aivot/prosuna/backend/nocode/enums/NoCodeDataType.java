package de.aivot.prosuna.backend.nocode.enums;

/**
 * Enum to represent the data types that can be used in the NoCode system.
 * Each data type has a key that is used to serialize the enum to JSON.
 */
public enum NoCodeDataType {
    Runtime,
    Boolean,
    Number,
    String,
    Date,
    DateTime,
    Time,
    List,
    Object,
}
