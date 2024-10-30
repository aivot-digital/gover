package de.aivot.GoverBackend.models.auth;

public class KeyCloakRealmMappings {
    private KeyCloakRole[] realmMappings;

    public KeyCloakRole[] getRealmMappings() {
        return realmMappings;
    }

    public void setRealmMappings(KeyCloakRole[] realmMappings) {
        this.realmMappings = realmMappings;
    }
}
