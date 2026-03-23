package de.aivot.GoverBackend.elements.models.elements.form.input;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class IdentityInputElementItem implements Serializable {
    @Nullable
    private UUID identityProviderKey;

    @Nonnull
    private Map<String, Object> identityData = new HashMap<>();

    // region Constructors
    // Empty constructor
    public IdentityInputElementItem() {

    }

    // Full constructor
    public IdentityInputElementItem(@Nullable UUID identityProviderKey,
                                    @Nonnull Map<String, Object> identityData) {
        this.identityProviderKey = identityProviderKey;
        this.identityData = identityData;
    }

    // endregion

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IdentityInputElementItem that = (IdentityInputElementItem) o;
        return Objects.equals(identityProviderKey, that.identityProviderKey) && Objects.equals(identityData, that.identityData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identityProviderKey, identityData);
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

    @Nonnull
    public Map<String, Object> getIdentityData() {
        return identityData;
    }

    public IdentityInputElementItem setIdentityData(@Nonnull Map<String, Object> identityData) {
        this.identityData = identityData;
        return this;
    }

    // endregion
}
