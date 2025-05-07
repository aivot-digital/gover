package de.aivot.GoverBackend.identity.models;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents an additional parameter passed as a query parameter to the OIDC authentication
 * endpoint during identity provider authentication.
 *
 * <p>This class is used to store key-value pairs where the key represents the name of the
 * query parameter, and the value represents its corresponding value. These parameters
 * allow for dynamic customization of the authentication request.</p>
 *
 * <p>Key functionalities:</p>
 * <ul>
 *     <li>Stores the key and value of a query parameter.</li>
 *     <li>Implements {@link Serializable} to support object serialization.</li>
 *     <li>Overrides {@link Object#equals(Object)} and {@link Object#hashCode()} to ensure
 *         proper comparison and hashing based on the key and value fields.</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 *     IdentityAdditionalParameter param = new IdentityAdditionalParameter();
 *     param.setKey("scope");
 *     param.setValue("openid email");
 * </pre>
 *
 * @see Serializable
 */
public class IdentityAdditionalParameter implements Serializable {
    private String key;
    private String value;

    // Equals and HashCode

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        IdentityAdditionalParameter that = (IdentityAdditionalParameter) object;
        return Objects.equals(key, that.key) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(key);
        result = 31 * result + Objects.hashCode(value);
        return result;
    }

    // endregion

    // Getters and Setters

    public String getKey() {
        return key;
    }

    public IdentityAdditionalParameter setKey(String key) {
        this.key = key;
        return this;
    }

    public String getValue() {
        return value;
    }

    public IdentityAdditionalParameter setValue(String value) {
        this.value = value;
        return this;
    }

    // endregion
}
