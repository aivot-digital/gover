package de.aivot.prosuna.backend.process.dtos;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * The instance is determined by the endpoint, never by a submitted rule.
 */
public record ProcessInstanceAccessRuleDTO(
        @Nullable Integer sourceDepartmentId,
        @Nullable Integer sourceTeamId,
        @Nonnull @NotNull List<@NotNull String> permissions
) {
}
