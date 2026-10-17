package de.aivot.prosuna.backend.ai.models;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record AiChatAttachmentMetadata(
        @Nonnull String name,
        long size,
        @Nullable String contentType
) {
}
