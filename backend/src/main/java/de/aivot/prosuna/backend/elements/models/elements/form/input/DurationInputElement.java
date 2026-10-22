package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.elements.models.elements.PrintableElement;
import de.aivot.prosuna.backend.enums.ConditionOperator;
import de.aivot.prosuna.backend.enums.DurationPrecision;
import de.aivot.prosuna.backend.enums.DurationLargestUnit;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.exceptions.RequiredValidationException;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import de.aivot.prosuna.backend.utils.DurationValueUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.util.Objects;

public class DurationInputElement extends BaseInputElement<BigDecimal> implements PrintableElement<BigDecimal> {
    @Nullable
    private DurationPrecision precision;
    @Nullable
    private DurationLargestUnit largestUnit;
    @Nullable
    private Long minDuration;
    @Nullable
    private Long maxDuration;

    public DurationInputElement() {
        super(ElementType.Duration);
    }

    @Nullable
    @Override
    public BigDecimal formatValue(@Nullable Object value) {
        if (value == null) return null;
        // Preserve fractions for validation; rounding here would silently change authored or derived data.
        try {
            if (value instanceof Number || value instanceof String) {
                return new BigDecimal(value.toString().trim());
            }
        } catch (NumberFormatException ignored) {
            // Use the field's public validation message instead of the parser's internals.
        }
        throw new IllegalArgumentException(DurationValueUtils.INVALID_DURATION);
    }

    @Override
    public void validate(@Nullable Object rawValue) throws ValidationException {
        try {
            super.validate(rawValue);
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(this, DurationValueUtils.INVALID_DURATION);
        }
    }

    @Override
    public void performValidation(@Nullable BigDecimal value) throws ValidationException {
        if (value == null) {
            if (Boolean.TRUE.equals(getRequired())) throw new RequiredValidationException(this);
            return;
        }
        final long seconds;
        try {
            seconds = DurationValueUtils.requireSeconds(value);
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(this, exception.getMessage());
        }
        if (precision != DurationPrecision.Second && seconds % 60 != 0) {
            throw new ValidationException(this, "Bitte geben Sie eine Zeitdauer in ganzen Minuten ein.");
        }
        if (minDuration != null && seconds < minDuration) {
            throw new ValidationException(this, "Die Zeitdauer muss mindestens " + formatBound(minDuration) + " betragen.");
        }
        if (maxDuration != null && seconds > maxDuration) {
            throw new ValidationException(this, "Die Zeitdauer darf höchstens " + formatBound(maxDuration) + " betragen.");
        }
    }

    @Nonnull
    private String formatBound(long seconds) {
        return DurationValueUtils.formatSeconds(seconds, precision != DurationPrecision.Second, largestUnit);
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable BigDecimal value) {
        return value == null ? "Keine Angabe" : formatBound(DurationValueUtils.requireSeconds(value));
    }

    @Nonnull
    @Override
    public Boolean evaluate(@Nonnull ConditionOperator operator, @Nullable Object referencedValue, @Nullable Object comparedValue) {
        var a = formatValue(referencedValue);
        if (operator == ConditionOperator.Empty) return a == null;
        if (operator == ConditionOperator.NotEmpty) return a != null;
        var b = formatValue(comparedValue);
        return switch (operator) {
            case Equals -> a == null ? b == null : b != null && a.compareTo(b) == 0;
            case NotEquals -> a == null ? b != null : b == null || a.compareTo(b) != 0;
            case LessThan -> a != null && b != null && a.compareTo(b) < 0;
            case LessThanOrEqual -> a != null && b != null && a.compareTo(b) <= 0;
            case GreaterThan -> a != null && b != null && a.compareTo(b) > 0;
            case GreaterThanOrEqual -> a != null && b != null && a.compareTo(b) >= 0;
            default -> false;
        };
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof DurationInputElement that && super.equals(other)
                && precision == that.precision && largestUnit == that.largestUnit
                && Objects.equals(minDuration, that.minDuration) && Objects.equals(maxDuration, that.maxDuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), precision, largestUnit, minDuration, maxDuration);
    }

    @Nullable
    public DurationPrecision getPrecision() {
        return precision;
    }

    @Nonnull
    public DurationInputElement setPrecision(@Nullable DurationPrecision precision) {
        this.precision = precision;
        return this;
    }

    @Nullable
    public DurationLargestUnit getLargestUnit() {
        return largestUnit;
    }

    @Nonnull
    public DurationInputElement setLargestUnit(@Nullable DurationLargestUnit largestUnit) {
        this.largestUnit = largestUnit;
        return this;
    }

    @Nullable
    public Long getMinDuration() {
        return minDuration;
    }

    @Nonnull
    public DurationInputElement setMinDuration(@Nullable Long minDuration) {
        this.minDuration = minDuration;
        return this;
    }

    @Nullable
    public Long getMaxDuration() {
        return maxDuration;
    }

    @Nonnull
    public DurationInputElement setMaxDuration(@Nullable Long maxDuration) {
        this.maxDuration = maxDuration;
        return this;
    }
}
