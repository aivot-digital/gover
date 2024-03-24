package de.aivot.GoverBackend.models.lib;

import java.io.Serializable;
import java.util.Objects;

public class PresetVersionKey implements Serializable {
    private String preset;

    private String version;

    public PresetVersionKey() {
    }

    public PresetVersionKey(String preset, String version) {
        this.preset = preset;
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PresetVersionKey that = (PresetVersionKey) o;

        if (!Objects.equals(preset, that.preset)) return false;
        return Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        int result = preset != null ? preset.hashCode() : 0;
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }

    public String getPreset() {
        return preset;
    }

    public void setPreset(String preset) {
        this.preset = preset;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
