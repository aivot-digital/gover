package de.aivot.GoverBackend.nocode.models;

import jakarta.annotation.Nullable;

import java.util.Objects;

public class NoCodeNodeDataReference extends NoCodeOperand {
    public static final String TYPE_ID = "NoCodeNodeDataReference";

    @Nullable
    private String nodeDataKey;
    @Nullable
    private String path;

    public NoCodeNodeDataReference() {
        super(TYPE_ID);
    }

    public NoCodeNodeDataReference(@Nullable String nodeDataKey, @Nullable String path) {
        super(TYPE_ID);
        this.nodeDataKey = nodeDataKey;
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        NoCodeNodeDataReference that = (NoCodeNodeDataReference) o;
        return Objects.equals(nodeDataKey, that.nodeDataKey)
                && Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nodeDataKey, path);
    }

    @Nullable
    public String getNodeDataKey() {
        return nodeDataKey;
    }

    public NoCodeNodeDataReference setNodeDataKey(@Nullable String nodeDataKey) {
        this.nodeDataKey = nodeDataKey;
        return this;
    }

    @Nullable
    public String getPath() {
        return path;
    }

    public NoCodeNodeDataReference setPath(@Nullable String path) {
        this.path = path;
        return this;
    }
}
