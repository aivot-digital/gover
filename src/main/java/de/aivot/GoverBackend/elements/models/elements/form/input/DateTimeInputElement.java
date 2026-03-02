package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.PrintableElement;
import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.enums.TimeType;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public class DateTimeInputElement extends BaseInputElement<ZonedDateTime> implements PrintableElement<ZonedDateTime> {
    private static final ZoneId zoneId = ZoneId.of("Europe/Berlin");

    @Nullable
    private String placeholder;
    @Nullable
    private TimeType mode;

    public DateTimeInputElement() {
        super(ElementType.DateTime);
    }

    @Nullable
    @Override
    public ZonedDateTime formatValue(@Nullable Object value) {
        return _formatValue(value);
    }

    @Override
    public void performValidation(@Nullable ZonedDateTime value) throws ValidationException {
        if (value == null && Boolean.TRUE.equals(getRequired())) {
            throw new RequiredValidationException(this);
        }
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable ZonedDateTime value) {
        if (value == null) {
            return "Keine Angabe";
        }

        return value
                .format(
                        DateTimeFormatter
                                .ofPattern(mode == TimeType.Second ? "dd.MM.yyyy HH:mm:ss" : "dd.MM.yyyy HH:mm")
                                .withZone(zoneId)
                ) + " Uhr";
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

        var tsA = dValA.toInstant().toEpochMilli();
        var tsB = dValB.toInstant().toEpochMilli();

        return switch (operator) {
            case Equals -> tsA == tsB;
            case NotEquals -> tsA != tsB;
            case LessThan -> tsA < tsB;
            case LessThanOrEqual -> tsA <= tsB;
            case GreaterThan -> tsA > tsB;
            case GreaterThanOrEqual -> tsA >= tsB;
            default -> false;
        };
    }

    private boolean evaluateRelative(ConditionOperator operator, ZonedDateTime value, String comparedValue) {
        int amount;
        try {
            amount = Integer.parseInt(comparedValue);
        } catch (NumberFormatException ex) {
            return false;
        }

        var now = ZonedDateTime.now(zoneId);

        return switch (operator) {
            case YearsInPast -> {
                var target = now.minusYears(amount);
                yield value.isBefore(target) || value.isEqual(target);
            }
            case MonthsInPast -> {
                var target = now.minusMonths(amount);
                yield value.isBefore(target) || value.isEqual(target);
            }
            case DaysInPast -> {
                var target = now.minusDays(amount);
                yield value.isBefore(target) || value.isEqual(target);
            }
            case YearsInFuture -> {
                var target = now.plusYears(amount);
                yield value.isAfter(target) || value.isEqual(target);
            }
            case MonthsInFuture -> {
                var target = now.plusMonths(amount);
                yield value.isAfter(target) || value.isEqual(target);
            }
            case DaysInFuture -> {
                var target = now.plusDays(amount);
                yield value.isAfter(target) || value.isEqual(target);
            }
            default -> false;
        };
    }

    @Nullable
    public static ZonedDateTime _formatValue(@Nullable Object value) {
        return switch (value) {
            case null -> null;
            case ZonedDateTime zValue -> zValue;
            case LocalDateTime lValue -> lValue.atZone(zoneId);
            case LocalDate ldValue -> ZonedDateTime.of(ldValue, LocalTime.now(), zoneId);
            case LocalTime lValue -> ZonedDateTime.of(LocalDate.now(), lValue, zoneId);
            case Instant iValue -> iValue.atZone(zoneId);
            case String sValue -> {
                try {
                    yield ZonedDateTime.parse(sValue);
                } catch (DateTimeException ex) {
                    try {
                        yield LocalDateTime.parse(sValue).atZone(zoneId);
                    } catch (DateTimeException ex1) {
                        try {
                            yield LocalDateTime.parse(sValue, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")).atZone(zoneId);
                        } catch (DateTimeParseException ex2) {
                            yield DateInputElement._formatValue(sValue);
                        }
                    }
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
