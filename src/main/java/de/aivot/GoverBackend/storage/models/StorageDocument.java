package de.aivot.GoverBackend.storage.models;

import jakarta.annotation.Nonnull;

public class StorageDocument extends StorageItem {
    @Nonnull
    private String extension;

    public StorageDocument(@Nonnull String pathFromRoot,
                           @Nonnull String name) {
        super(pathFromRoot, name, false);

        int lastDotIndex = getName().lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == getName().length() - 1) {
            extension = "";
        }
        extension = getName().substring(lastDotIndex + 1);
    }

    @Nonnull
    public String getExtension() {
        return extension;
    }

    public StorageDocument setExtension(@Nonnull String extension) {
        this.extension = extension;
        return this;
    }
}
