package de.aivot.GoverBackend.mail.dtos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record TestMailResponseDTO(
        @Nonnull
        Boolean success,
        @Nullable
        String errorMessage
) {
    @Nonnull
    public static TestMailResponseDTO createSuccess() {
        return new TestMailResponseDTO(true, null);
    }

    @Nonnull
    public static TestMailResponseDTO createError(@Nonnull Throwable error) {
        return new TestMailResponseDTO(false, error.getMessage());
    }
}
