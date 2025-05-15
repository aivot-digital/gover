package de.aivot.GoverBackend.secrets.entities;

import de.aivot.GoverBackend.secrets.properties.SecretConfigurationProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * This class represents a secret entity in the database.
 * Each secret has a unique key, name, description, value, and salt.
 * The value of the secret is encrypted using the salt and a secret key.
 * The salt is used to prevent rainbow table attacks on the encrypted value.
 * The secret key is stored in the application configuration file.
 * The secret key should be a secure and random string of characters.
 * See {@link SecretConfigurationProperties} for more information.
 */
@Entity
@Table(name = "secrets")
public class SecretEntity {
    @Id
    @Column(length = 36, columnDefinition = "uuid")
    private String key;

    @Column(length = 64)
    @NotNull(message = "Name cannot be null")
    @NotBlank(message = "Name cannot be blank")
    private String name;

    @Column(length = 255)
    @NotNull(message = "Description cannot be null")
    @NotBlank(message = "Description cannot be blank")
    private String description;

    @NotNull(message = "Value cannot be null")
    @Column(columnDefinition = "TEXT")
    private String value;

    @Column(length = 16)
    @NotNull(message = "Salt cannot be null")
    @NotBlank(message = "Salt cannot be blank")
    private String salt;

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        SecretEntity that = (SecretEntity) object;
        return (
                Objects.equals(key, that.key) &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(value, that.value) &&
                Objects.equals(salt, that.salt)
        );
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(key);
        result = 31 * result + Objects.hashCode(name);
        result = 31 * result + Objects.hashCode(description);
        result = 31 * result + Objects.hashCode(value);
        result = 31 * result + Objects.hashCode(salt);
        return result;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
