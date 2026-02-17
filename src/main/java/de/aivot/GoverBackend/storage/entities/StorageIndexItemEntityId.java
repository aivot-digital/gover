package de.aivot.GoverBackend.storage.entities;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Objects;

public class StorageIndexItemEntityId {
    @Id
    @Nonnull
    private Integer storageProviderId;

    @Id
    @Nonnull
    private String pathFromRoot;

    public StorageIndexItemEntityId() {
        this.storageProviderId = 0;
        this.pathFromRoot = "";
    }

    public StorageIndexItemEntityId(@Nonnull Integer storageProviderId, @Nonnull String pathFromRoot) {
        this.storageProviderId = storageProviderId;
        this.pathFromRoot = pathFromRoot;
    }

    public static StorageIndexItemEntityId of(Integer storageProviderId, String pathFromRoot) {
        return new StorageIndexItemEntityId(storageProviderId, pathFromRoot);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        StorageIndexItemEntityId that = (StorageIndexItemEntityId) o;
        return Objects.equals(storageProviderId, that.storageProviderId) && Objects.equals(pathFromRoot, that.pathFromRoot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storageProviderId, pathFromRoot);
    }

    @Nonnull
    public Integer getStorageProviderId() {
        return storageProviderId;
    }

    public StorageIndexItemEntityId setStorageProviderId(@Nonnull Integer storageProviderId) {
        this.storageProviderId = storageProviderId;
        return this;
    }

    @Nonnull
    public String getPathFromRoot() {
        return pathFromRoot;
    }

    public StorageIndexItemEntityId setPathFromRoot(@Nonnull String pathFromRoot) {
        this.pathFromRoot = pathFromRoot;
        return this;
    }
}
