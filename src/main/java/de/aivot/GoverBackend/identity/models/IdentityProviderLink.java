package de.aivot.GoverBackend.identity.models;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Represents a link between an entity and an identity provider.
 *
 * <p>This class is used to associate entities with identity providers and
 * to transport additional information (e.g., minimum authentication levels)
 * to the identity provider. It provides a way to define the relationship
 * between an entity and an identity provider, along with any additional
 * scopes required for the interaction.</p>
 *
 * <p>Key functionalities:</p>
 * <ul>
 *     <li>Stores the unique key of the identity provider.</li>
 *     <li>Stores additional scopes that may include information such as
 *         minimum authentication levels or other requirements.</li>
 *     <li>Implements {@link Serializable} to support object serialization.</li>
 *     <li>Overrides {@link Object#equals(Object)} and {@link Object#hashCode()} to ensure
 *         proper comparison and hashing based on the identity provider key and additional scopes.</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 *     IdentityProviderLink link = new IdentityProviderLink();
 *     link.setIdentityProviderKey("provider1");
 *     link.setAdditionalScopes(List.of("scope1", "scope2"));
 * </pre>
 *
 * @see Serializable
 */
public class IdentityProviderLink implements Serializable {
    @Nullable
    private String identityProviderKey;

    @Nullable
    private List<String> additionalScopes;

    // region Equals & HashCode

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        IdentityProviderLink that = (IdentityProviderLink) object;
        return Objects.equals(identityProviderKey, that.identityProviderKey) && Objects.equals(additionalScopes, that.additionalScopes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(identityProviderKey);
        result = 31 * result + Objects.hashCode(additionalScopes);
        return result;
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public String getIdentityProviderKey() {
        return identityProviderKey;
    }

    public IdentityProviderLink setIdentityProviderKey(@Nullable String identityProviderKey) {
        this.identityProviderKey = identityProviderKey;
        return this;
    }

    @Nullable
    public List<String> getAdditionalScopes() {
        return additionalScopes;
    }

    public IdentityProviderLink setAdditionalScopes(@Nullable List<String> additionalScopes) {
        this.additionalScopes = additionalScopes;
        return this;
    }

    // endregion
}
