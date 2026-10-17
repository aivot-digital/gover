package de.aivot.prosuna.backend.elements.models.elements;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

public class ElementMetadata implements Serializable {
    private String identitySourceId;
    private Map<String, String> identityMappings;
    private String userInfoIdentifier;

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ElementMetadata that = (ElementMetadata) o;
        return Objects.equals(identitySourceId, that.identitySourceId) && Objects.equals(identityMappings, that.identityMappings) &&
                Objects.equals(userInfoIdentifier, that.userInfoIdentifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identitySourceId, identityMappings, userInfoIdentifier);
    }

    // endregion

    // region Getters & Setters

    public String getIdentitySourceId() {
        return identitySourceId;
    }

    public ElementMetadata setIdentitySourceId(String identitySourceId) {
        this.identitySourceId = identitySourceId;
        return this;
    }

    public Map<String, String> getIdentityMappings() {
        return identityMappings;
    }

    public ElementMetadata setIdentityMappings(Map<String, String> identityMappings) {
        this.identityMappings = identityMappings;
        return this;
    }

    public String getUserInfoIdentifier() {
        return userInfoIdentifier;
    }

    public ElementMetadata setUserInfoIdentifier(String userInfoIdentifier) {
        this.userInfoIdentifier = userInfoIdentifier;
        return this;
    }

    // endregion
}
