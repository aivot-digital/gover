package de.aivot.GoverBackend.nocode.models;

import jakarta.annotation.Nullable;

import java.util.Objects;

public class NoCodeInstanceDataReference extends NoCodeOperand {
    public static final String TYPE_ID = "NoCodeInstanceDataReference";

    @Nullable
    private String path;

    public NoCodeInstanceDataReference() {
        super(TYPE_ID);
    }

    public NoCodeInstanceDataReference(@Nullable String path) {
        super(TYPE_ID);
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        NoCodeInstanceDataReference that = (NoCodeInstanceDataReference) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), path);
    }

    @Nullable
    public String getPath() {
        return path;
    }

    public NoCodeInstanceDataReference setPath(@Nullable String path) {
        this.path = path;
        return this;
    }
}
