package de.aivot.GoverBackend.form.entities;

import jakarta.annotation.Nonnull;

public class VFormVersionWithDetailsEntityId {
    @Nonnull
    private Integer id;
    @Nonnull
    private Integer version;

    public VFormVersionWithDetailsEntityId() {
        this.id = 0;
        this.version = 0;
    }

    public VFormVersionWithDetailsEntityId(@Nonnull Integer id, @Nonnull Integer version) {
        this.id = id;
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        VFormVersionWithDetailsEntityId that = (VFormVersionWithDetailsEntityId) o;
        return id.equals(that.id) && version.equals(that.version);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }

    @Nonnull
    public Integer getId() {
        return id;
    }

    public VFormVersionWithDetailsEntityId setId(@Nonnull Integer id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public Integer getVersion() {
        return version;
    }

    public VFormVersionWithDetailsEntityId setVersion(@Nonnull Integer version) {
        this.version = version;
        return this;
    }
}
