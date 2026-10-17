package de.aivot.prosuna.backend.lib;

public interface RequestDTO<T> {
    T toEntity();
}
