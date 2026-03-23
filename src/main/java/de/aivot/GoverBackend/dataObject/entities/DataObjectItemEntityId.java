package de.aivot.GoverBackend.dataObject.entities;

import jakarta.annotation.Nonnull;

public class DataObjectItemEntityId {
    @Nonnull
    private String schemaKey;
    @Nonnull
    private String id;

    public DataObjectItemEntityId() {
        this.schemaKey = "";
        this.id = "";
    }

    public DataObjectItemEntityId(@Nonnull String schemaKey, @Nonnull String id) {
        this.schemaKey = schemaKey;
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        DataObjectItemEntityId that = (DataObjectItemEntityId) o;
        return schemaKey.equals(that.schemaKey) && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        int result = schemaKey.hashCode();
        result = 31 * result + id.hashCode();
        return result;
    }

    @Nonnull
    public String getSchemaKey() {
        return schemaKey;
    }

    public DataObjectItemEntityId setSchemaKey(@Nonnull String schemaKey) {
        this.schemaKey = schemaKey;
        return this;
    }

    @Nonnull
    public String getId() {
        return id;
    }

    public DataObjectItemEntityId setId(@Nonnull String id) {
        this.id = id;
        return this;
    }
}
