package de.aivot.GoverBackend.elements.models.elements;

import java.util.Map;
import java.util.Objects;

public class ElementMetadata {
    private Map<String, String> identityMappings;
    private String userInfoIdentifier;

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        ElementMetadata that = (ElementMetadata) o;
        return Objects.equals(identityMappings, that.identityMappings) && Objects.equals(userInfoIdentifier, that.userInfoIdentifier);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(identityMappings);
        result = 31 * result + Objects.hashCode(userInfoIdentifier);
        return result;
    }

    // endregion

    // region Getters & Setters

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
