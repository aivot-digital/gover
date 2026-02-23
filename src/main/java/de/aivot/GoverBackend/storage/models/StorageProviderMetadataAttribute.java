package de.aivot.GoverBackend.storage.models;

import java.io.Serializable;

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
}
