package de.aivot.GoverBackend.lib.models;

public interface Identifiable<T> {
    T getKey();
    boolean matches(Object other);
}
