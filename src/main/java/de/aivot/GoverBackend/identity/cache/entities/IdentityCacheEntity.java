package de.aivot.GoverBackend.identity.cache.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.util.Map;

@RedisHash(value = "CacheIdentity", timeToLive = 60 * 60 *4) // Expire after 4 hours
public class IdentityCacheEntity implements Serializable {
    @Id
    private String id;
    private String providerKey;
    private Map<String, String> identityData;

    // region Getters and Setters

    public String getId() {
        return id;
    }

    public IdentityCacheEntity setId(String id) {
        this.id = id;
        return this;
    }

    public String getProviderKey() {
        return providerKey;
    }

    public IdentityCacheEntity setProviderKey(String providerKey) {
        this.providerKey = providerKey;
        return this;
    }

    public Map<String, String> getIdentityData() {
        return identityData;
    }

    public IdentityCacheEntity setIdentityData(Map<String, String> identityData) {
        this.identityData = identityData;
        return this;
    }

    // endregion
}
