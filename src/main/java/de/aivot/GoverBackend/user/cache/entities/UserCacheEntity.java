package de.aivot.GoverBackend.user.cache.entities;

import de.aivot.GoverBackend.user.entities.UserEntity;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.time.LocalDateTime;

@RedisHash(value = "CacheUser", timeToLive = 30) // Expire after 30 seconds
public class UserCacheEntity implements Serializable {
    @Id
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private Boolean enabled;
    private Boolean verified;
    private Boolean globalAdmin;
    private Boolean deletedInIdp;

    // region Builders

    public static UserCacheEntity from(@Nonnull UserEntity entity) {
        return new UserCacheEntity()
                .setId(entity.getId())
                .setEmail(entity.getEmail())
                .setFirstName(entity.getFirstName())
                .setLastName(entity.getLastName())
                .setFullName(entity.getFullName())
                .setEnabled(entity.getEnabled())
                .setVerified(entity.getVerified())
                .setGlobalAdmin(entity.getGlobalAdmin())
                .setDeletedInIdp(entity.getDeletedInIdp());
    }

    // endregion

    // region Getters and Setters

    public String getId() {
        return id;
    }

    public UserCacheEntity setId(String id) {
        this.id = id;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public UserCacheEntity setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public UserCacheEntity setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public UserCacheEntity setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public UserCacheEntity setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public Boolean getVerified() {
        return verified;
    }

    public UserCacheEntity setVerified(Boolean verified) {
        this.verified = verified;
        return this;
    }

    public Boolean getGlobalAdmin() {
        return globalAdmin;
    }

    public UserCacheEntity setGlobalAdmin(Boolean globalAdmin) {
        this.globalAdmin = globalAdmin;
        return this;
    }

    public Boolean getDeletedInIdp() {
        return deletedInIdp;
    }

    public UserCacheEntity setDeletedInIdp(Boolean deletedInIdp) {
        this.deletedInIdp = deletedInIdp;
        return this;
    }

    public String getFullName() {
        return fullName;
    }

    public UserCacheEntity setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    // endregion
}
