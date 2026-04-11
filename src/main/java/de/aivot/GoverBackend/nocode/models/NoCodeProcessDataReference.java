package de.aivot.GoverBackend.nocode.models;

import jakarta.annotation.Nullable;

import java.util.Objects;

public class NoCodeProcessDataReference extends NoCodeOperand {
    public static final String TYPE_ID = "NoCodeProcessDataReference";

    @Nullable
    private String path;

    public NoCodeProcessDataReference() {
        super(TYPE_ID);
    }

    public NoCodeProcessDataReference(@Nullable String path) {
        super(TYPE_ID);
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        NoCodeProcessDataReference that = (NoCodeProcessDataReference) o;
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

    public NoCodeProcessDataReference setPath(@Nullable String path) {
        this.path = path;
        return this;
    }
}
