package de.aivot.GoverBackend.user.entities;

import de.aivot.GoverBackend.user.cache.entities.UserCacheEntity;
import de.aivot.GoverBackend.user.models.KeycloakUser;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "users")
public class UserEntity {
    private static final String ADMIN_ROLE_IDENTIFIER = "admin";
    public static final Integer DEFAULT_USER_ROLE_VALUE = 0;
    public static final Integer SYSTEM_ADMIN_ROLE_VALUE = 1;
    public static final Integer SUPER_ADMIN_ROLE_VALUE = 2;

    @Id
    @Nonnull
    @Column(length = 36)
    private String id;

    @Nullable
    @Column(length = 255)
    private String email;

    @Nullable
    @Column(length = 255)
    private String firstName;

    @Nullable
    @Column(length = 255)
    private String lastName;

    @Nullable
    @Column(length = 255)
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

    @Nonnull
    @ColumnDefault("0")
    private Integer globalRole;

    // Properties

    public Boolean getSuperAdmin() {
        return globalRole >= SUPER_ADMIN_ROLE_VALUE;
    }

    public Boolean getSystemAdmin() {
        return globalRole >= SYSTEM_ADMIN_ROLE_VALUE;
    }

    // region Transformers

    public Optional<UserEntity> asGlobalAdmin() {
        if (getSuperAdmin()) {
            return Optional.of(this);
        } else {
            return Optional.empty();
        }
    }

    public Optional<UserEntity> asSystemAdmin() {
        if (getSystemAdmin() || getSuperAdmin()) {
            return Optional.of(this);
        } else {
            return Optional.empty();
        }
    }

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

        fullName = firstName + " " + lastName;

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

    // endregion

    // region Utils

    public boolean hasId(String id) {
        return this.id.equals(id);
    }

    // endregion

    // region Builders

    public static UserEntity from(@Nonnull Jwt jwt) {
        var user = new UserEntity()
                .setId(jwt.getClaimAsString("sub"))
                .setEmail(jwt.getClaimAsString("email"))
                .setFirstName(jwt.getClaimAsString("given_name"))
                .setLastName(jwt.getClaimAsString("family_name"))
                .setFullName(jwt.getClaimAsString("given_name") + " " + jwt.getClaimAsString("family_name"))
                .setEnabled(true) // Users with a valid JWT are always enabled
                .setVerified(jwt.getClaimAsBoolean("verified"))
                .setGlobalRole(DEFAULT_USER_ROLE_VALUE) // Set default value for later check
                .setDeletedInIdp(false); // Users with a valid JWT are never deleted in the IDP

        var realmAccessMap = jwt.getClaimAsMap("realm_access");
        if (realmAccessMap != null) {
            var realmAccessRoles = realmAccessMap.get("roles");
            if (realmAccessRoles instanceof List<?> realmAccessRoleList) {
                for (Object role : realmAccessRoleList) {
                    if (role.toString().equalsIgnoreCase(ADMIN_ROLE_IDENTIFIER)) {
                        user.setGlobalRole(SUPER_ADMIN_ROLE_VALUE);
                        break;
                    }
                }
            }
        }

        return user;
    }

    public static UserEntity from(@Nonnull UserCacheEntity userCacheEntity) {
        return new UserEntity()
                .setId(userCacheEntity.getId())
                .setEmail(userCacheEntity.getEmail())
                .setFirstName(userCacheEntity.getFirstName())
                .setLastName(userCacheEntity.getLastName())
                .setFullName(userCacheEntity.getFullName())
                .setEnabled(userCacheEntity.getEnabled())
                .setVerified(userCacheEntity.getVerified())
                .setGlobalRole(userCacheEntity.getGlobalAdmin() ? SUPER_ADMIN_ROLE_VALUE : DEFAULT_USER_ROLE_VALUE)
                .setDeletedInIdp(userCacheEntity.getDeletedInIdp());
    }

    public static UserEntity from(@Nonnull KeycloakUser keycloakUser, @Nonnull List<String> roles) {
        return new UserEntity()
                .setId(keycloakUser.getId())
                .setEmail(keycloakUser.getEmail())
                .setFirstName(keycloakUser.getFirstName())
                .setLastName(keycloakUser.getLastName())
                .setFullName(keycloakUser.getFirstName() + " " + keycloakUser.getLastName())
                .setEnabled(keycloakUser.getEnabled())
                .setVerified(keycloakUser.getEmailVerified())
                .setGlobalRole(roles.stream().anyMatch(role -> role.equalsIgnoreCase(ADMIN_ROLE_IDENTIFIER)) ? SUPER_ADMIN_ROLE_VALUE : DEFAULT_USER_ROLE_VALUE)
                .setDeletedInIdp(false);
    }

    // endregion

    // region Getters and Setters

    public String getId() {
        return id;
    }

    public UserEntity setId(String id) {
        this.id = id;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public UserEntity setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public UserEntity setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public UserEntity setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getFullName() {
        return fullName;
    }

    public UserEntity setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public UserEntity setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public Boolean getVerified() {
        return verified;
    }

    public UserEntity setVerified(Boolean verified) {
        this.verified = verified;
        return this;
    }

    public Boolean getDeletedInIdp() {
        return deletedInIdp;
    }

    public UserEntity setDeletedInIdp(Boolean deletedInIdp) {
        this.deletedInIdp = deletedInIdp;
        return this;
    }

    public Integer getGlobalRole() {
        return globalRole;
    }

    public UserEntity setGlobalRole(Integer globalRole) {
        this.globalRole = globalRole;
        return this;
    }

    // endregion
}
