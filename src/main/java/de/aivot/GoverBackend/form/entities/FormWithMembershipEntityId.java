package de.aivot.GoverBackend.form.entities;

public class FormWithMembershipEntityId {
    private Integer id;
    private String userId;

    public FormWithMembershipEntityId() {
    }

    public FormWithMembershipEntityId(Integer id, String userId) {
        this.id = id;
        this.userId = userId;
    }

    public Integer getId() {
        return id;
    }

    public FormWithMembershipEntityId setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public FormWithMembershipEntityId setUserId(String userId) {
        this.userId = userId;
        return this;
    }
}
