package de.aivot.prosuna.backend.ai.cache.entities;

import de.aivot.prosuna.backend.enums.ElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@RedisHash(value = "AiUiElementChatSession", timeToLive = 60 * 60 * 4) // Expire after 4 hours
public class AiUiElementChatSessionCacheEntity implements Serializable {
    @Id
    @Nonnull
    private String id;

    @Nullable
    private ElementType targetRootType;

    @Nullable
    // Store the tree as JSON because Redis hash mapping cannot handle lists nested in Map<String, Object> values.
    private String currentElementJson;

    @Nonnull
    public String getId() {
        return id;
    }

    public AiUiElementChatSessionCacheEntity setId(@Nonnull String id) {
        this.id = id;
        return this;
    }

    @Nullable
    public String getCurrentElementJson() {
        return currentElementJson;
    }

    public AiUiElementChatSessionCacheEntity setCurrentElementJson(@Nullable String currentElementJson) {
        this.currentElementJson = currentElementJson;
        return this;
    }

    @Nullable
    public ElementType getTargetRootType() {
        return targetRootType;
    }

    public AiUiElementChatSessionCacheEntity setTargetRootType(@Nullable ElementType targetRootType) {
        this.targetRootType = targetRootType;
        return this;
    }
}
