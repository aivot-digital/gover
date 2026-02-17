package de.aivot.GoverBackend.form.entities;

import jakarta.annotation.Nonnull;

public class VFormWithPermissionsEntityId {
    @Nonnull
    private Integer id;
    @Nonnull
    private String userId;

    public VFormWithPermissionsEntityId() {
        this.id = 0;
        this.userId = "";
    }

    public VFormWithPermissionsEntityId(@Nonnull Integer id, @Nonnull String userId) {
        this.id = id;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        VFormWithPermissionsEntityId that = (VFormWithPermissionsEntityId) o;
        return id.equals(that.id) && userId.equals(that.userId);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + userId.hashCode();
        return result;
    }

    @Nonnull
    public Integer getId() {
        return id;
    }

    public VFormWithPermissionsEntityId setId(@Nonnull Integer id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public String getUserId() {
        return userId;
    }

    public VFormWithPermissionsEntityId setUserId(@Nonnull String userId) {
        this.userId = userId;
        return this;
    }
}
