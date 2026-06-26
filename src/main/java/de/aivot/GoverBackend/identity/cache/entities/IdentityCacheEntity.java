package de.aivot.GoverBackend.identity.cache.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RedisHash(value = "CacheIdentity", timeToLive = 60 * 60 * 4) // Expire after 4 hours
public class IdentityCacheEntity implements Serializable {
    @Id
    @Nonnull
    private String id;
    @Nonnull
    @Indexed
    private String sessionId = "";
    @Nonnull
    @Indexed
    private Integer relatedProcessNodeId;
    @Nullable
    private String codeVerifier;
    @Nonnull
    private UUID providerKey = UUID.randomUUID();
    @Nonnull
    private String identityId = "";
    @Nonnull
    private String metadataIdentifier = "";
    @Nonnull
    private String origin = "";
    @Nonnull
    private String stateNonce = "";
    @Nullable
    private Map<String, String> identityData;

    // region Constructors
    public IdentityCacheEntity() {
    }

    public IdentityCacheEntity(@Nonnull String id,
                               @Nonnull String sessionId,
                               @Nonnull Integer relatedProcessNodeId,
                               @Nullable String codeVerifier,
                               @Nonnull UUID providerKey,
                               @Nonnull String identityId,
                               @Nonnull String metadataIdentifier,
                               @Nonnull String origin,
                               @Nonnull String stateNonce,
                               @Nullable Map<String, String> identityData) {
        this.id = id;
        this.sessionId = sessionId;
        this.relatedProcessNodeId = relatedProcessNodeId;
        this.codeVerifier = codeVerifier;
        this.providerKey = providerKey;
        this.identityId = identityId;
        this.metadataIdentifier = metadataIdentifier;
        this.origin = origin;
        this.stateNonce = stateNonce;
        this.identityData = identityData;
    }

    // endregion

    // region HashCode and Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IdentityCacheEntity that = (IdentityCacheEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(sessionId, that.sessionId) &&
                Objects.equals(relatedProcessNodeId, that.relatedProcessNodeId) && Objects.equals(codeVerifier, that.codeVerifier) &&
                Objects.equals(providerKey, that.providerKey) && Objects.equals(identityId, that.identityId) &&
                Objects.equals(metadataIdentifier, that.metadataIdentifier) && Objects.equals(origin, that.origin) &&
                Objects.equals(stateNonce, that.stateNonce) && Objects.equals(identityData, that.identityData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sessionId, relatedProcessNodeId, codeVerifier, providerKey, identityId, metadataIdentifier, origin, stateNonce, identityData);
    }


    // endregion

    // region Getters and Setters

    @Nonnull
    public String getId() {
        return id;
    }

    public IdentityCacheEntity setId(@Nonnull String id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public String getSessionId() {
        return sessionId;
    }

    public IdentityCacheEntity setSessionId(@Nonnull String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    @Nonnull
    public Integer getRelatedProcessNodeId() {
        return relatedProcessNodeId;
    }

    public IdentityCacheEntity setRelatedProcessNodeId(@Nonnull Integer relatedProcessNodeId) {
        this.relatedProcessNodeId = relatedProcessNodeId;
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
    public UUID getProviderKey() {
        return providerKey;
    }

    public IdentityCacheEntity setProviderKey(@Nonnull UUID providerKey) {
        this.providerKey = providerKey;
        return this;
    }

    @Nonnull
    public String getIdentityId() {
        return identityId;
    }

    public IdentityCacheEntity setIdentityId(@Nonnull String identityId) {
        this.identityId = identityId;
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

    @Nonnull
    public String getOrigin() {
        return origin;
    }

    public IdentityCacheEntity setOrigin(@Nonnull String origin) {
        this.origin = origin;
        return this;
    }

    @Nonnull
    public String getStateNonce() {
        return stateNonce;
    }

    public IdentityCacheEntity setStateNonce(@Nonnull String stateNonce) {
        this.stateNonce = stateNonce;
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
