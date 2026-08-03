package de.aivot.gover.backend.elements.models.elements.form.input;

import de.aivot.gover.backend.elements.models.elements.BaseInputElement;
import de.aivot.gover.backend.elements.models.elements.PrintableElement;
import de.aivot.gover.backend.enums.ConditionOperator;
import de.aivot.gover.backend.enums.ElementType;
import de.aivot.gover.backend.enums.TimeType;
import de.aivot.gover.backend.exceptions.RequiredValidationException;
import de.aivot.gover.backend.exceptions.ValidationException;
import de.aivot.gover.backend.utils.ApplicationTimeZone;
import de.aivot.gover.backend.utils.IsoTimestampUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class DateTimeInputElement extends BaseInputElement<Instant> implements PrintableElement<Instant> {
    @Nullable
    private String placeholder;
    @Nullable
    private TimeType mode;

    public DateTimeInputElement() {
        super(ElementType.DateTime);
    }

    @Nullable
    @Override
    public Instant formatValue(@Nullable Object value) {
        // Unlike a floating LocalTime, an Instant keeps its full source precision.
        // The field mode only limits user input and display; truncating here would alter
        // programmatically supplied absolute timestamps.
        return _formatValue(value);
    }

    @Override
    public void performValidation(@Nullable Instant value) throws ValidationException {
        if (value == null && Boolean.TRUE.equals(getRequired())) {
            throw new RequiredValidationException(this);
        }
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable Instant value) {
        if (value == null) {
            return "Keine Angabe";
        }

        return DateTimeFormatter
                .ofPattern(mode == TimeType.Second ? "dd.MM.yyyy HH:mm:ss" : "dd.MM.yyyy HH:mm")
                .withZone(ApplicationTimeZone.getZoneId())
                .format(value) + " Uhr";
    }

    @Nonnull
    @Override
    public Boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        if (referencedValue == null) {
            return switch (operator) {
                case Equals -> comparedValue == null;
                case NotEquals -> comparedValue != null;
                case Empty -> true;
                default -> false;
            };
        }

        if (operator == ConditionOperator.NotEmpty) {
            return true;
        }

        var dValA = _formatValue(referencedValue);
        if (dValA == null) {
            return false;
        }

        String sValB = comparedValue instanceof String s ? s : null;
        if (sValB == null) {
            return false;
        }

        switch (operator) {
            case YearsInPast:
            case MonthsInPast:
            case DaysInPast:
            case YearsInFuture:
            case MonthsInFuture:
            case DaysInFuture:
                return evaluateRelative(operator, dValA, sValB);
            default:
                break;
        }

        var dValB = _formatValue(sValB);
        if (dValB == null) {
            return false;
        }

        var comparison = dValA.compareTo(dValB);

        return switch (operator) {
            case Equals -> comparison == 0;
            case NotEquals -> comparison != 0;
            case LessThan -> comparison < 0;
            case LessThanOrEqual -> comparison <= 0;
            case GreaterThan -> comparison > 0;
            case GreaterThanOrEqual -> comparison >= 0;
            default -> false;
        };
    }

    private boolean evaluateRelative(ConditionOperator operator, Instant value, String comparedValue) {
        int amount;
        try {
            amount = Integer.parseInt(comparedValue);
        } catch (NumberFormatException ex) {
            return false;
        }

        var zonedValue = value.atZone(ApplicationTimeZone.getZoneId());
        var now = ZonedDateTime.now(ApplicationTimeZone.getZoneId());

        return switch (operator) {
            case YearsInPast -> {
                var target = now.minusYears(amount);
                yield !zonedValue.isAfter(target);
            }
            case MonthsInPast -> {
                var target = now.minusMonths(amount);
                yield !zonedValue.isAfter(target);
            }
            case DaysInPast -> {
                var target = now.minusDays(amount);
                yield !zonedValue.isAfter(target);
            }
            case YearsInFuture -> {
                var target = now.plusYears(amount);
                yield !zonedValue.isBefore(target);
            }
            case MonthsInFuture -> {
                var target = now.plusMonths(amount);
                yield !zonedValue.isBefore(target);
            }
            case DaysInFuture -> {
                var target = now.plusDays(amount);
                yield !zonedValue.isBefore(target);
            }
            default -> false;
        };
    }

    @Nullable
    public static Instant _formatValue(@Nullable Object value) {
        return switch (value) {
            case null -> null;
            case Instant instant -> instant;
            case ZonedDateTime zonedDateTime -> zonedDateTime.toInstant();
            case OffsetDateTime offsetDateTime -> offsetDateTime.toInstant();
            case String sValue -> {
                try {
                    yield IsoTimestampUtils.parseIsoInstant(sValue);
                } catch (DateTimeException ex) {
                    yield null;
                }
            }
            default -> null;
        };
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        DateTimeInputElement that = (DateTimeInputElement) o;
        return Objects.equals(placeholder, that.placeholder) && mode == that.mode;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(placeholder);
        result = 31 * result + Objects.hashCode(mode);
        return result;
    }

    @Nullable
    public String getPlaceholder() {
        return placeholder;
    }

    public DateTimeInputElement setPlaceholder(@Nullable String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    @Nullable
    public TimeType getMode() {
        return mode;
    }

    public DateTimeInputElement setMode(@Nullable TimeType mode) {
        this.mode = mode;
        return this;
    }

}
