package de.aivot.GoverBackend.identity.cache.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RedisHash(value = "CacheIdentity", timeToLive = 60 * 60 * 4) // Expire after 4 hours
public class IdentityCacheEntity implements Serializable {
    @Id
    @Nonnull
    private UUID sessionId;
    @Nullable
    private String codeVerifier;
    @Nonnull
    private String providerKey = "";
    @Nonnull
    private String metadataIdentifier = "";
    @Nullable
    private Map<String, String> identityData;

    // region Constructors
    public IdentityCacheEntity() {
    }

    public IdentityCacheEntity(@Nonnull UUID sessionId,
                               @Nullable String codeVerifier,
                               @Nonnull String providerKey,
                               @Nonnull String metadataIdentifier,
                               @Nullable Map<String, String> identityData) {
        this.sessionId = sessionId;
        this.codeVerifier = codeVerifier;
        this.providerKey = providerKey;
        this.metadataIdentifier = metadataIdentifier;
        this.identityData = identityData;
    }

    // endregion

    // region HashCode and Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        IdentityCacheEntity that = (IdentityCacheEntity) o;
        return sessionId.equals(that.sessionId) && Objects.equals(codeVerifier, that.codeVerifier) && providerKey.equals(that.providerKey) && metadataIdentifier.equals(that.metadataIdentifier) && Objects.equals(identityData, that.identityData);
    }

    @Override
    public int hashCode() {
        int result = sessionId.hashCode();
        result = 31 * result + Objects.hashCode(codeVerifier);
        result = 31 * result + providerKey.hashCode();
        result = 31 * result + metadataIdentifier.hashCode();
        result = 31 * result + Objects.hashCode(identityData);
        return result;
    }


    // endregion

    // region Getters and Setters

    @Nonnull
    public UUID getSessionId() {
        return sessionId;
    }

    public IdentityCacheEntity setSessionId(@Nonnull UUID sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    @Nullable
    public String getCodeVerifier() {
        return codeVerifier;
    }

    public IdentityCacheEntity setCodeVerifier(@Nullable String codeVerifier) {
        this.codeVerifier = codeVerifier;
        return this;
    }

    @Nonnull
    public String getProviderKey() {
        return providerKey;
    }

    public IdentityCacheEntity setProviderKey(@Nonnull String providerKey) {
        this.providerKey = providerKey;
        return this;
    }

    @Nonnull
    public String getMetadataIdentifier() {
        return metadataIdentifier;
    }

    public IdentityCacheEntity setMetadataIdentifier(@Nonnull String metadataIdentifier) {
        this.metadataIdentifier = metadataIdentifier;
        return this;
    }

    @Nullable
    public Map<String, String> getIdentityData() {
        return identityData;
    }

    public IdentityCacheEntity setIdentityData(@Nullable Map<String, String> identityData) {
        this.identityData = identityData;
        return this;
    }

    // endregion
}
