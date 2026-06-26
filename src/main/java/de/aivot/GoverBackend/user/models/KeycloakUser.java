package de.aivot.GoverBackend.user.models;

import de.aivot.GoverBackend.user.entities.UserEntity;

import java.util.List;

public class KeycloakUser {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private Boolean emailVerified;
    private Boolean enabled;
    private List<String> requiredActions;

    public static KeycloakUser from(UserEntity entity) {
        return new KeycloakUser()
                .setId(entity.getId())
                .setFirstName(entity.getFirstName())
                .setLastName(entity.getLastName())
                .setEmail(entity.getEmail())
                .setEmailVerified(entity.getVerified())
                .setEnabled(entity.getEnabled());
    }

    public String getId() {
        return id;
    }

    public KeycloakUser setId(String id) {
        this.id = id;
        return this;
    }

    public String getFirstName() {
        if (firstName == null) {
            return "";
        }
        return firstName;
    }

    public KeycloakUser setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        if (lastName == null) {
            return "";
        }
        return lastName;
    }

    public KeycloakUser setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getEmail() {
        if (email == null) {
            return "";
        }
        return email;
    }

    public KeycloakUser setEmail(String email) {
        this.email = email;
        return this;
    }

    public Boolean getEmailVerified() {
        if (emailVerified == null) {
            return false;
        }
        return emailVerified;
    }

    public KeycloakUser setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
        return this;
    }

    public Boolean getEnabled() {
        if (enabled == null) {
            return false;
        }
        return enabled;
    }

    public KeycloakUser setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public List<String> getRequiredActions() {
        return requiredActions;
    }

    public KeycloakUser setRequiredActions(List<String> requiredActions) {
        this.requiredActions = requiredActions;
        return this;
    }
}
