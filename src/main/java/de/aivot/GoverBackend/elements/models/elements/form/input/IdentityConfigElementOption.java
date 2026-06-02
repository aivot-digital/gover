package de.aivot.GoverBackend.elements.models.elements.form.input;

import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class IdentityConfigElementOption implements Serializable {
    @Nullable
    private UUID identityProviderKey;

    @Nullable
    private List<String> additionalScopes;

    // region Constructors

    // Empty constructor
    public IdentityConfigElementOption() {

    }

    // Full constructor

    public IdentityConfigElementOption(@Nullable UUID identityProviderKey,
                                       @Nullable List<String> additionalScopes) {
        this.identityProviderKey = identityProviderKey;
        this.additionalScopes = additionalScopes;
    }

    // endregion

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IdentityConfigElementOption that = (IdentityConfigElementOption) o;
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

    public IdentityConfigElementOption setIdentityProviderKey(@Nullable UUID identityProviderKey) {
        this.identityProviderKey = identityProviderKey;
        return this;
    }

    @Nullable
    public List<String> getAdditionalScopes() {
        return additionalScopes;
    }

    public IdentityConfigElementOption setAdditionalScopes(@Nullable List<String> additionalScopes) {
        this.additionalScopes = additionalScopes;
        return this;
    }

    // endregion
}
