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

    // region Constructors

    // Empty constructor
    public IdentityInputElementOption() {

    }

    // Full constructor
    public IdentityInputElementOption(@Nullable UUID identityProviderKey,
                                      @Nullable List<String> additionalScopes) {
        this.identityProviderKey = identityProviderKey;
        this.additionalScopes = additionalScopes;
    }

    // endregion

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IdentityInputElementOption that = (IdentityInputElementOption) o;
        return Objects.equals(identityProviderKey, that.identityProviderKey) && Objects.equals(additionalScopes, that.additionalScopes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identityProviderKey, additionalScopes);
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

    // endregion
}
