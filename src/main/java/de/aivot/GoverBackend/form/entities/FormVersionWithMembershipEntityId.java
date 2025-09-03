package de.aivot.GoverBackend.form.entities;

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
