package de.aivot.prosuna.backend.nocode.dtos;

import de.aivot.prosuna.backend.nocode.enums.NoCodeDryRunStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Result of one operand in a dry run. The results of an expression's operands follow its operand order.
 *
 * @param status   Whether the operand was evaluated.
 * @param value    The value of an evaluated operand.
 * @param message  The reason a failed or skipped operand has no value.
 * @param operands The results of an expression's operands, empty for other operands.
 */
public record NoCodeDryRunResultDTO(
        @Nonnull NoCodeDryRunStatus status,
        @Nullable Object value,
        @Nullable String message,
        @Nonnull List<NoCodeDryRunResultDTO> operands
) {
    public static NoCodeDryRunResultDTO evaluated(@Nullable Object value, @Nonnull List<NoCodeDryRunResultDTO> operands) {
        return new NoCodeDryRunResultDTO(NoCodeDryRunStatus.Evaluated, value, null, operands);
    }

    public static NoCodeDryRunResultDTO failed(@Nonnull String message, @Nonnull List<NoCodeDryRunResultDTO> operands) {
        return new NoCodeDryRunResultDTO(NoCodeDryRunStatus.Failed, null, message, operands);
    }

    public static NoCodeDryRunResultDTO skipped(@Nonnull String message, @Nonnull List<NoCodeDryRunResultDTO> operands) {
        return new NoCodeDryRunResultDTO(NoCodeDryRunStatus.Skipped, null, message, operands);
    }
}
