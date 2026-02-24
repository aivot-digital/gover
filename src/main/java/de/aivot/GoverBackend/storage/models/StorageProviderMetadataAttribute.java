package de.aivot.GoverBackend.storage.models;

import java.io.Serializable;
import java.util.Objects;

public class StorageProviderMetadataAttribute implements Serializable {
    private String key;
    private String label;
    private String description;

    public String getKey() {
        return key;
    }

    public StorageProviderMetadataAttribute setKey(String key) {
        this.key = key;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public StorageProviderMetadataAttribute setLabel(String label) {
        this.label = label;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public StorageProviderMetadataAttribute setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        StorageProviderMetadataAttribute that = (StorageProviderMetadataAttribute) o;
        return Objects.equals(key, that.key) && Objects.equals(label, that.label) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, label, description);
    }
}
