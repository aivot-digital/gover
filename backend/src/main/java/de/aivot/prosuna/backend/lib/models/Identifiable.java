package de.aivot.prosuna.backend.lib.models;

public interface Identifiable<T> {
    T getKey();
    boolean matches(Object other);
}
