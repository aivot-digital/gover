package de.aivot.GoverBackend.form.entities;

public class FormVersionWithMembershipEntityId {
    private Integer id;
    private String version;
    private String userId;

    public FormVersionWithMembershipEntityId() {
    }

    public FormVersionWithMembershipEntityId(Integer id, String version, String userId) {
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

    public String getVersion() {
        return version;
    }

    public FormVersionWithMembershipEntityId setVersion(String version) {
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
