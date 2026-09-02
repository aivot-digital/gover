package de.aivot.prosuna.backend.elements.models.elements.form.input;

import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Objects;

public class StoragePathSelectorInputElementValue implements Serializable {
    @Nullable
    private Integer storageProviderId;

    @Nullable
    private String path;

    @Nullable
    public Integer getStorageProviderId() {
        return storageProviderId;
    }

    public StoragePathSelectorInputElementValue setStorageProviderId(@Nullable Integer storageProviderId) {
        this.storageProviderId = storageProviderId;
        return this;
    }

    @Nullable
    public String getPath() {
        return path;
    }

    public StoragePathSelectorInputElementValue setPath(@Nullable String path) {
        this.path = path;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        StoragePathSelectorInputElementValue that = (StoragePathSelectorInputElementValue) o;
        return Objects.equals(storageProviderId, that.storageProviderId)
                && Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storageProviderId, path);
    }
}
