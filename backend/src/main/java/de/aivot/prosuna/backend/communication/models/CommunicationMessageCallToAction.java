package de.aivot.prosuna.backend.communication.models;

import jakarta.annotation.Nonnull;

public record CommunicationMessageCallToAction(
        @Nonnull
        String title,
        @Nonnull
        String link
) {
}
