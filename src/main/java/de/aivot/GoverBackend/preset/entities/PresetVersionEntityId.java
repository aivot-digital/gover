package de.aivot.GoverBackend.preset.entities;

import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.util.UUID;

public class PresetVersionEntityId implements Serializable {
    @Nonnull
    private UUID presetKey;

    @Nonnull
    private Integer version;

    public PresetVersionEntityId() {
        presetKey = UUID.randomUUID();
        version = 1;
    }

    public PresetVersionEntityId(@Nonnull UUID presetKey, @Nonnull Integer version) {
        this.presetKey = presetKey;
        this.version = version;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        PresetVersionEntityId that = (PresetVersionEntityId) object;
        return presetKey.equals(that.presetKey) && version.equals(that.version);
    }

    @Override
    public int hashCode() {
        int result = presetKey.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }

    @Nonnull
    public UUID getPresetKey() {
        return presetKey;
    }

    public PresetVersionEntityId setPresetKey(@Nonnull UUID presetKey) {
        this.presetKey = presetKey;
        return this;
    }

    @Nonnull
    public Integer getVersion() {
        return version;
    }

    public PresetVersionEntityId setVersion(@Nonnull Integer version) {
        this.version = version;
        return this;
    }
}
