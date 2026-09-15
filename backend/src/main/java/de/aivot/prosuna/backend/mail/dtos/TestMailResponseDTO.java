package de.aivot.prosuna.backend.mail.dtos;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record TestMailResponseDTO(
        boolean success,
        @Nullable
        String errorMessage
) {
    @Nonnull
    public static TestMailResponseDTO createSuccess() {
        return new TestMailResponseDTO(true, null);
    }

    @Nonnull
    public static TestMailResponseDTO createError(@Nonnull Throwable error) {
        var current = error;
        while ((current.getMessage() == null || current.getMessage().isBlank()) && current.getCause() != null) {
            current = current.getCause();
        }

        var message = current.getMessage();
        if (message == null || message.isBlank()) {
            message = current.getClass().getSimpleName();
        }

        return new TestMailResponseDTO(false, message);
    }
}
