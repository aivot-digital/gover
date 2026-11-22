package de.aivot.prosuna.backend.nocode.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDryRunStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
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
    /** Temporal values serialize as JSON strings. Preserve their type for the editor before serialization. */
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public NoCodeDataType getValueType() {
        if (value instanceof LocalDate || value instanceof YearMonth || value instanceof Year) {
            return NoCodeDataType.Date;
        }
        if (value instanceof Instant || value instanceof OffsetDateTime || value instanceof ZonedDateTime || value instanceof LocalDateTime) {
            return NoCodeDataType.DateTime;
        }
        if (value instanceof LocalTime) {
            return NoCodeDataType.Time;
        }
        return null;
    }

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
