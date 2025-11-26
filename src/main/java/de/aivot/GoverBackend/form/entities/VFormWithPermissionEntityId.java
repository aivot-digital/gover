package de.aivot.GoverBackend.form.entities;

import java.util.Objects;

public class VFormWithPermissionEntityId {
    private Integer id;
    private String userId;

    public VFormWithPermissionEntityId() {
    }

    public VFormWithPermissionEntityId(Integer id, String userId) {
        this.id = id;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        VFormWithPermissionEntityId that = (VFormWithPermissionEntityId) object;
        return Objects.equals(id, that.id) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(userId);
        return result;
    }

    public Integer getId() {
        return id;
    }

    public VFormWithPermissionEntityId setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public VFormWithPermissionEntityId setUserId(String userId) {
        this.userId = userId;
        return this;
    }
}
