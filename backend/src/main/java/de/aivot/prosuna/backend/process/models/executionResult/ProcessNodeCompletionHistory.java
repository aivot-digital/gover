package de.aivot.prosuna.backend.process.models.executionResult;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/** Describes an outcome that becomes history only after the task result has been accepted. */
public record ProcessNodeCompletionHistory(
        @Nonnull String title,
        @Nonnull String message,
        @Nullable String remark,
        @Nullable String concernedUserId,
        @Nullable String concernedIdentityId
) {
}
