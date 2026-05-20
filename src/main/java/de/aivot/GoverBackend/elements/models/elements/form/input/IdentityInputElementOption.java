package de.aivot.GoverBackend.elements.models.elements.form.input;

import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class IdentityInputElementOption implements Serializable {
    @Nullable
    private UUID identityProviderKey;

    @Nullable
    private List<String> additionalScopes;

    @Nullable
    private List<IdentityInputElementOptionAttributeMapping> attributeMappings;

    // region Constructors

    // Empty constructor
    public IdentityInputElementOption() {

    }

    // Full constructor

    public IdentityInputElementOption(@Nullable UUID identityProviderKey,
                                      @Nullable List<String> additionalScopes,
                                      @Nullable List<IdentityInputElementOptionAttributeMapping> attributeMappings) {
        this.identityProviderKey = identityProviderKey;
        this.additionalScopes = additionalScopes;
        this.attributeMappings = attributeMappings;
    }

    // endregion

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IdentityInputElementOption that = (IdentityInputElementOption) o;
        return Objects.equals(identityProviderKey, that.identityProviderKey) && Objects.equals(additionalScopes, that.additionalScopes) &&
                Objects.equals(attributeMappings, that.attributeMappings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identityProviderKey, additionalScopes, attributeMappings);
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public UUID getIdentityProviderKey() {
        return identityProviderKey;
    }

    public IdentityInputElementOption setIdentityProviderKey(@Nullable UUID identityProviderKey) {
        this.identityProviderKey = identityProviderKey;
        return this;
    }

    @Nullable
    public List<String> getAdditionalScopes() {
        return additionalScopes;
    }

    public IdentityInputElementOption setAdditionalScopes(@Nullable List<String> additionalScopes) {
        this.additionalScopes = additionalScopes;
        return this;
    }

    @Nullable
    public List<IdentityInputElementOptionAttributeMapping> getAttributeMappings() {
        return attributeMappings;
    }

    public IdentityInputElementOption setAttributeMappings(@Nullable List<IdentityInputElementOptionAttributeMapping> attributeMappings) {
        this.attributeMappings = attributeMappings;
        return this;
    }


    // endregion
}
