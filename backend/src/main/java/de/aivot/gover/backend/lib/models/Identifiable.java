package de.aivot.gover.backend.lib.models;

public interface Identifiable<T> {
    T getKey();
    boolean matches(Object other);
}
