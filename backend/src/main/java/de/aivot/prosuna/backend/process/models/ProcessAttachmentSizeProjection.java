package de.aivot.prosuna.backend.process.models;

import jakarta.annotation.Nonnull;

import java.util.UUID;

public interface ProcessAttachmentSizeProjection {
    @Nonnull
    UUID getKey();

    @Nonnull
    Long getSizeInBytes();
}
