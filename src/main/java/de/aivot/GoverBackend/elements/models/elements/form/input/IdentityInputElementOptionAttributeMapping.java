package de.aivot.GoverBackend.elements.models.elements.form.input;

import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class IdentityInputElementOptionAttributeMapping implements Serializable {
    @Nullable
    private String fromIdentityProviderAttribute;

    @Nullable
    private String toFormElementWithId;

    // region Constructors

    // Empty constructor
    public IdentityInputElementOptionAttributeMapping() {

    }

    // Full constructor
    public IdentityInputElementOptionAttributeMapping(@Nullable String fromIdentityProviderAttribute,
                                                      @Nullable String toFormElementWithId) {
        this.fromIdentityProviderAttribute = fromIdentityProviderAttribute;
        this.toFormElementWithId = toFormElementWithId;
    }

    // endregion

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IdentityInputElementOptionAttributeMapping that = (IdentityInputElementOptionAttributeMapping) o;
        return Objects.equals(fromIdentityProviderAttribute, that.fromIdentityProviderAttribute) && Objects.equals(toFormElementWithId, that.toFormElementWithId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromIdentityProviderAttribute, toFormElementWithId);
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public String getFromIdentityProviderAttribute() {
        return fromIdentityProviderAttribute;
    }

    public IdentityInputElementOptionAttributeMapping setFromIdentityProviderAttribute(@Nullable String fromIdentityProviderAttribute) {
        this.fromIdentityProviderAttribute = fromIdentityProviderAttribute;
        return this;
    }

    @Nullable
    public String getToFormElementWithId() {
        return toFormElementWithId;
    }

    public IdentityInputElementOptionAttributeMapping setToFormElementWithId(@Nullable String toFormElementWithId) {
        this.toFormElementWithId = toFormElementWithId;
        return this;
    }


    // endregion
}
