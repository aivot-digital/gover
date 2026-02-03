package de.aivot.GoverBackend.storage.models;

import jakarta.annotation.Nonnull;

public abstract class StorageItem {
    /**
     * The pathFromRoot to this item, based on the storage provider's root.
     */
    @Nonnull
    private String pathFromRoot;

    /**
     * The name of this item.
     */
    @Nonnull
    private String name;

    /**
     * Weather this item is a folder.
     */
    private boolean isFolder;

    public StorageItem(@Nonnull String pathFromRoot,
                       @Nonnull String name,
                       boolean isFolder) {
        this.pathFromRoot = pathFromRoot;
        this.name = name;
        this.isFolder = isFolder;
    }

    @Nonnull
    public String getPathFromRoot() {
        return pathFromRoot;
    }

    public StorageItem setPathFromRoot(@Nonnull String pathFromRoot) {
        this.pathFromRoot = pathFromRoot;
        return this;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public StorageItem setName(@Nonnull String name) {
        this.name = name;
        return this;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public StorageItem setFolder(boolean folder) {
        isFolder = folder;
        return this;
    }
}
