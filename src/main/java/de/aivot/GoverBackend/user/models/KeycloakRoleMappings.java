package de.aivot.GoverBackend.user.models;

import java.util.List;

public class KeycloakRoleMappings {
    private List<KeycloakRoleMapping> realmMappings;

    public List<KeycloakRoleMapping> getRealmMappings() {
        return realmMappings;
    }

    public KeycloakRoleMappings setRealmMappings(List<KeycloakRoleMapping> realmMappings) {
        this.realmMappings = realmMappings;
        return this;
    }
}
