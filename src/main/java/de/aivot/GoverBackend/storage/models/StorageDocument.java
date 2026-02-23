package de.aivot.GoverBackend.storage.models;

import jakarta.annotation.Nonnull;

public class StorageDocument extends StorageItem {
    @Nonnull
    private String extension;

    @Nonnull
    private StorageItemMetadata metadata;

    public StorageDocument(@Nonnull String pathFromRoot,
                           @Nonnull String name) {
        this(pathFromRoot, name, new StorageItemMetadata());
    }

    public StorageDocument(@Nonnull String pathFromRoot,
                           @Nonnull String name,
                           @Nonnull StorageItemMetadata metadata) {
        super(pathFromRoot, name, false);

        int lastDotIndex = getName().lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == getName().length() - 1) {
            extension = "";
        }
        extension = getName().substring(lastDotIndex + 1);
        this.metadata = metadata;
    }

    @Nonnull
    public String getExtension() {
        return extension;
    }

    public StorageDocument setExtension(@Nonnull String extension) {
        this.extension = extension;
        return this;
    }

    @Nonnull
    public StorageItemMetadata getMetadata() {
        return metadata;
    }

    public StorageDocument setMetadata(@Nonnull StorageItemMetadata metadata) {
        this.metadata = metadata;
        return this;
    }
}
