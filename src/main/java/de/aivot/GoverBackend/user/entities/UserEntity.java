package de.aivot.GoverBackend.user.entities;

import de.aivot.GoverBackend.user.models.KeycloakUser;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;

import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @Nonnull
    @Column(length = 36)
    private String id;

    @Nullable
    @Column(length = 255)
    @NotNull(message = "Die E-Mail Adresse darf nicht null sein.")
    @Email(message = "Die E-Mail Adresse muss gültig sein.")
    @Size(min = 8, max = 255, message = "Die E-Mail Adresse muss zwischen 8 und 255 Zeichen lang sein.")
    private String email;

    @Nullable
    @Column(length = 255)
    @NotNull(message = "Der Vorname darf nicht null sein.")
    @Size(min = 1, max = 255, message = "Der Vorname muss zwischen 1 und 255 Zeichen lang sein.")
    private String firstName;

    @Nullable
    @Column(length = 255)
    @NotNull(message = "Der Nachname darf nicht null sein.")
    @Size(min = 1, max = 255, message = "Der Nachname muss zwischen 1 und 255 Zeichen lang sein.")
    private String lastName;

    @Nonnull
    @Column(insertable = false, updatable = false)
    private String fullName;

    @Nonnull
    @ColumnDefault("FALSE")
    private Boolean enabled;

    @Nonnull
    @ColumnDefault("FALSE")
    private Boolean verified;

    @Nonnull
    @ColumnDefault("FALSE")
    private Boolean deletedInIdp;

    @Nullable
    @ColumnDefault("null")
    private Integer systemRoleId;

    // region Properties

    /**
     * @deprecated TODO: REMOVE
     * @return
     */
    @Deprecated
    public Boolean getIsSuperAdmin() {
        return true;
    }

    /**
     * @deprecated TODO: REMOVE
     * @return
     */
    @Deprecated
    public Boolean getIsSystemAdmin() {
        return true;
    }

    // endregion

    // region Transformers

    public Optional<UserEntity> asSuperAdmin() {
        if (getIsSuperAdmin()) {
            return Optional.of(this);
        } else {
            return Optional.empty();
        }
    }

    public Optional<UserEntity> asSystemAdmin() {
        if (getIsSystemAdmin() || getIsSuperAdmin()) {
            return Optional.of(this);
        } else {
            return Optional.empty();
        }
    }

    // endregion

    // region Utils

    public void clearPersonalData() {
        int maskLength = 6;

        if (StringUtils.isNotNullOrEmpty(firstName)) {
            firstName = firstName.charAt(0) + "*".repeat(maskLength);
        } else {
            firstName = "?";
        }

        if (StringUtils.isNotNullOrEmpty(lastName)) {
            lastName = lastName.charAt(0) + "*".repeat(maskLength);
        } else {
            lastName = "?";
        }

        if (StringUtils.isNotNullOrEmpty(email) && email.contains("@")) {
            String[] parts = email.split("@");
            if (!parts[0].isEmpty()) {
                email = parts[0].charAt(0) + "*".repeat(maskLength) + "@" + parts[1];
            } else {
                email = "*".repeat(maskLength) + "@" + parts[1];
            }
        } else {
            email = "?";
        }
    }

    public boolean hasId(String id) {
        return this.id.equals(id);
    }

    // endregion

    // region Builders

    public static UserEntity from(@Nonnull KeycloakUser keycloakUser) {
        return new UserEntity()
                .setId(keycloakUser.getId())
                .setEmail(keycloakUser.getEmail())
                .setFirstName(keycloakUser.getFirstName())
                .setLastName(keycloakUser.getLastName())
                .setEnabled(keycloakUser.getEnabled())
                .setVerified(keycloakUser.getEmailVerified())
                .setSystemRoleId(null)
                .setDeletedInIdp(false);
    }

    // endregion

    // region Getters and Setters

    @Nonnull
    public String getId() {
        return id;
    }

    public UserEntity setId(@Nonnull String id) {
        this.id = id;
        return this;
    }

    @Nullable
    public String getEmail() {
        return email;
    }

    public UserEntity setEmail(@Nullable String email) {
        this.email = email;
        return this;
    }

    @Nullable
    public String getFirstName() {
        return firstName;
    }

    public UserEntity setFirstName(@Nullable String firstName) {
        this.firstName = firstName;
        return this;
    }

    @Nullable
    public String getLastName() {
        return lastName;
    }

    public UserEntity setLastName(@Nullable String lastName) {
        this.lastName = lastName;
        return this;
    }

    @Nonnull
    public Boolean getEnabled() {
        return enabled;
    }

    public UserEntity setEnabled(@Nonnull Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    @Nonnull
    public Boolean getVerified() {
        return verified;
    }

    public UserEntity setVerified(@Nonnull Boolean verified) {
        this.verified = verified;
        return this;
    }

    @Nonnull
    public Boolean getDeletedInIdp() {
        return deletedInIdp;
    }

    public UserEntity setDeletedInIdp(@Nonnull Boolean deletedInIdp) {
        this.deletedInIdp = deletedInIdp;
        return this;
    }

    @Nullable
    public Integer getSystemRoleId() {
        return systemRoleId;
    }

    public UserEntity setSystemRoleId(@Nullable Integer globalRole) {
        this.systemRoleId = globalRole;
        return this;
    }

    @Nonnull
    public String getFullName() {
        return fullName;
    }

    public UserEntity setFullName(@Nonnull String fullName) {
        this.fullName = fullName;
        return this;
    }

    // endregion
}
