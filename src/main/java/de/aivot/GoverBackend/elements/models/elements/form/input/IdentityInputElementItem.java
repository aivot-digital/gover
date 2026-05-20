package de.aivot.GoverBackend.elements.models.elements.form.input;

import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class IdentityInputElementItem implements Serializable {
    @Nullable
    private UUID identityProviderKey;

    @Nullable
    private Map<String, Object> identityAttributes;

    // region Constructors
    // Empty constructor
    public IdentityInputElementItem() {

    }

    // Full constructor
    public IdentityInputElementItem(@Nullable UUID identityProviderKey,
                                    @Nullable Map<String, Object> identityAttributes) {
        this.identityProviderKey = identityProviderKey;
        this.identityAttributes = identityAttributes;
    }

    // endregion

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IdentityInputElementItem that = (IdentityInputElementItem) o;
        return Objects.equals(identityProviderKey, that.identityProviderKey) && Objects.equals(identityAttributes, that.identityAttributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identityProviderKey, identityAttributes);
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public UUID getIdentityProviderKey() {
        return identityProviderKey;
    }

    public IdentityInputElementItem setIdentityProviderKey(@Nullable UUID identityProviderKey) {
        this.identityProviderKey = identityProviderKey;
        return this;
    }

    @Nullable
    public Map<String, Object> getIdentityAttributes() {
        return identityAttributes;
    }

    public IdentityInputElementItem setIdentityAttributes(@Nullable Map<String, Object> identityAttributes) {
        this.identityAttributes = identityAttributes;
        return this;
    }

    // endregion
}
