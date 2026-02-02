package de.aivot.GoverBackend.storage.models;

import jakarta.annotation.Nonnull;

public record StorageDocument(
        @Nonnull
        String pathFromRoot,
        @Nonnull
        String filename
) {
    public String extension() {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}
