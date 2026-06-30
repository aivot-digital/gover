package de.aivot.gover.backend.lib;

public interface RequestDTO<T> {
    T toEntity();
}
