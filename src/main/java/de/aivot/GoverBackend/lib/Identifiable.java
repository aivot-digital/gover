package de.aivot.GoverBackend.lib;

public interface Identifiable<T> {
    public T getKey();
    public boolean matches(Object other);
}
