package de.aivot.GoverBackend.lib;

public interface Identifiable<T> {
    T getKey();
    boolean matches(Object other);
}
