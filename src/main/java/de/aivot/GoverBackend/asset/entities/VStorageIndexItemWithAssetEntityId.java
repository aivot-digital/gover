package de.aivot.GoverBackend.asset.entities;

import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.util.Objects;

public class VStorageIndexItemWithAssetEntityId implements Serializable {
    @Nonnull
    private final Integer storageProviderId;

    @Nonnull
    private final String pathFromRoot;

    public VStorageIndexItemWithAssetEntityId() {
        this.storageProviderId = 0;
        this.pathFromRoot = "";
    }

    public VStorageIndexItemWithAssetEntityId(@Nonnull Integer storageProviderId,
                                              @Nonnull String pathFromRoot) {
        this.storageProviderId = storageProviderId;
        this.pathFromRoot = pathFromRoot;
    }

    public static VStorageIndexItemWithAssetEntityId of(Integer storageProviderId, String pathFromRoot) {
        return new VStorageIndexItemWithAssetEntityId(storageProviderId, pathFromRoot);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        VStorageIndexItemWithAssetEntityId that = (VStorageIndexItemWithAssetEntityId) o;
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

    @Nonnull
    public String getPathFromRoot() {
        return pathFromRoot;
    }
}
