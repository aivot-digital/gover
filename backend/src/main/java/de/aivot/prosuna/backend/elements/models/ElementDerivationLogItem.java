package de.aivot.prosuna.backend.elements.models;

import de.aivot.prosuna.backend.elements.enums.ElementDerivationLogLevel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.Map;

public class ElementDerivationLogItem {
    @Nonnull
    private Instant timestamp;
    @Nonnull
    private ElementDerivationLogLevel level;
    @Nonnull
    private String elementId;
    @Nonnull
    private String message;
    @Nullable
    private Map<String, Object> details;

    public ElementDerivationLogItem(@Nonnull String elementId,
                                    @Nonnull ElementDerivationLogLevel level,
                                    @Nonnull String message,
                                    @Nullable Map<String, Object> details) {
        this.timestamp = Instant.now();
        this.elementId = elementId;
        this.level = level;
        this.message = message;
        this.details = details;
    }

    @Nonnull
    public Instant getTimestamp() {
        return timestamp;
    }

    public ElementDerivationLogItem setTimestamp(@Nonnull Instant timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    @Nonnull
    public String getElementId() {
        return elementId;
    }

    public ElementDerivationLogItem setElementId(@Nonnull String elementId) {
        this.elementId = elementId;
        return this;
    }

    @Nonnull
    public ElementDerivationLogLevel getLevel() {
        return level;
    }

    public ElementDerivationLogItem setLevel(@Nonnull ElementDerivationLogLevel level) {
        this.level = level;
        return this;
    }

    @Nonnull
    public String getMessage() {
        return message;
    }

    public ElementDerivationLogItem setMessage(@Nonnull String message) {
        this.message = message;
        return this;
    }

    @Nullable
    public Map<String, Object> getDetails() {
        return details;
    }

    public ElementDerivationLogItem setDetails(@Nullable Map<String, Object> details) {
        this.details = details;
        return this;
    }
}
