package de.aivot.GoverBackend.form.entities;

import java.util.Objects;

public class FormVersionWithMembershipEntityId {
    private Integer id;
    private Integer version;
    private String userId;

    public FormVersionWithMembershipEntityId() {
    }

    public FormVersionWithMembershipEntityId(Integer id, Integer version, String userId) {
        this.id = id;
        this.version = version;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        FormVersionWithMembershipEntityId that = (FormVersionWithMembershipEntityId) object;
        return Objects.equals(id, that.id) && Objects.equals(version, that.version) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(version);
        result = 31 * result + Objects.hashCode(userId);
        return result;
    }

    public Integer getId() {
        return id;
    }

    public FormVersionWithMembershipEntityId setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public FormVersionWithMembershipEntityId setVersion(Integer version) {
        this.version = version;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public FormVersionWithMembershipEntityId setUserId(String userId) {
        this.userId = userId;
        return this;
    }
}
