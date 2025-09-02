package de.aivot.GoverBackend.lib;

public interface RequestDTO<T> {
    T toEntity();
}
