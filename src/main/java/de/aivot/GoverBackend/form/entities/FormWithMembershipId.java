package de.aivot.GoverBackend.form.entities;

import java.util.Objects;

public class FormWithMembershipId {
    private Integer id;
    private String userId;

    public FormWithMembershipId() {
    }

    public FormWithMembershipId(Integer id, String userId) {
        this.id = id;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        FormWithMembershipId that = (FormWithMembershipId) object;
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

    public FormWithMembershipId setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public FormWithMembershipId setUserId(String userId) {
        this.userId = userId;
        return this;
    }
}
