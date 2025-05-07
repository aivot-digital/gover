package de.aivot.GoverBackend.identity.models;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a mapping option between identity provider user information and fields in the Gover system's form editor.
 *
 * <p>This class is used to display possible attributes of the identity provider userinfo to staff members designing forms and to map
 * identity provider data onto form input fields. It provides a user-friendly label and description
 * for the mapping, as well as a key to identify the corresponding value in the identity provider's
 * user information dataset.</p>
 *
 * <p>Key functionalities:</p>
 * <ul>
 *     <li>Stores a label and description for display in the form editor.</li>
 *     <li>Stores a key used to identify and map values from the identity provider's user information dataset.</li>
 *     <li>Implements {@link Serializable} to support object serialization.</li>
 *     <li>Overrides {@link Object#equals(Object)} and {@link Object#hashCode()} to ensure
 *         proper comparison and hashing based on the label, description, and keyInData fields.</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 *     IdentityAttributeMapping mapping = new IdentityAttributeMapping();
 *     mapping.setLabel("Email");
 *     mapping.setDescription("The user's email address");
 *     mapping.setKeyInData("email");
 *     mapping.setDisplayAttribute(false);
 * </pre>
 *
 * @see Serializable
 */
public class IdentityAttributeMapping implements Serializable {
    private String label;
    private String description;
    private String keyInData;
    private Boolean displayAttribute;

    // Equals and HashCode

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        IdentityAttributeMapping that = (IdentityAttributeMapping) object;
        return Objects.equals(label, that.label) &&
               Objects.equals(description, that.description) &&
               Objects.equals(keyInData, that.keyInData) &&
               Objects.equals(displayAttribute, that.displayAttribute);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(label);
        result = 31 * result + Objects.hashCode(description);
        result = 31 * result + Objects.hashCode(keyInData);
        result = 31 * result + Objects.hashCode(displayAttribute);
        return result;
    }

    // endregion

    // Getters and Setters

    public String getLabel() {
        return label;
    }

    public IdentityAttributeMapping setLabel(String label) {
        this.label = label;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public IdentityAttributeMapping setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getKeyInData() {
        return keyInData;
    }

    public IdentityAttributeMapping setKeyInData(String keyInData) {
        this.keyInData = keyInData;
        return this;
    }

    public Boolean getDisplayAttribute() {
        return displayAttribute;
    }

    public IdentityAttributeMapping setDisplayAttribute(Boolean displayAttribute) {
        this.displayAttribute = displayAttribute;
        return this;
    }

    // endregion
}
