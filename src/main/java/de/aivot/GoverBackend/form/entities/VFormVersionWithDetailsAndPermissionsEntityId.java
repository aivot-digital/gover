package de.aivot.GoverBackend.form.entities;

import jakarta.annotation.Nonnull;

public class VFormVersionWithDetailsAndPermissionsEntityId {
    @Nonnull
    private Integer id;
    @Nonnull
    private Integer version;
    @Nonnull
    private String userId;

    public VFormVersionWithDetailsAndPermissionsEntityId() {
        this.id = 0;
        this.version = 0;
        this.userId = "";
    }

    public VFormVersionWithDetailsAndPermissionsEntityId(@Nonnull Integer id,
                                                         @Nonnull Integer version,
                                                         @Nonnull String userId) {
        this.id = id;
        this.version = version;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        VFormVersionWithDetailsAndPermissionsEntityId that = (VFormVersionWithDetailsAndPermissionsEntityId) o;
        return id.equals(that.id) && version.equals(that.version) && userId.equals(that.userId);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + version.hashCode();
        result = 31 * result + userId.hashCode();
        return result;
    }

    @Nonnull
    public Integer getId() {
        return id;
    }

    public VFormVersionWithDetailsAndPermissionsEntityId setId(@Nonnull Integer id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public Integer getVersion() {
        return version;
    }

    public VFormVersionWithDetailsAndPermissionsEntityId setVersion(@Nonnull Integer version) {
        this.version = version;
        return this;
    }

    @Nonnull
    public String getUserId() {
        return userId;
    }

    public VFormVersionWithDetailsAndPermissionsEntityId setUserId(@Nonnull String userId) {
        this.userId = userId;
        return this;
    }
}
