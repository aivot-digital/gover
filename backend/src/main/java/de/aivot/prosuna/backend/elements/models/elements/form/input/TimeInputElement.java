package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.elements.models.elements.PrintableElement;
import de.aivot.prosuna.backend.enums.ConditionOperator;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.enums.TimeType;
import de.aivot.prosuna.backend.exceptions.RequiredValidationException;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import de.aivot.prosuna.backend.utils.ApplicationTimeZone;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.regex.Pattern;

public class TimeInputElement extends BaseInputElement<LocalTime> implements PrintableElement<LocalTime> {
    @Nullable
    private TimeType mode;

    public TimeInputElement() {
        super(ElementType.Time);
    }

    @Override
    public LocalTime formatValue(Object value) {
        var localTime = switch (value) {
            case null -> null;
            case LocalTime time -> time;
            case LocalDateTime dateTime -> dateTime.toLocalTime();
            case Instant instant -> instant.atZone(ApplicationTimeZone.getZoneId()).toLocalTime();
            case OffsetDateTime dateTime -> dateTime
                    .toInstant()
                    .atZone(ApplicationTimeZone.getZoneId())
                    .toLocalTime();
            case ZonedDateTime dateTime -> dateTime
                    .withZoneSameInstant(ApplicationTimeZone.getZoneId())
                    .toLocalTime();
            case String string -> parseLocalTime(string);
            default -> null;
        };

        if (localTime == null) {
            return null;
        }

        // Minute precision is an input constraint. Normalize hidden seconds here so
        // a value can never contain data that the field does not expose to the user.
        localTime = localTime.withNano(0);
        return mode == TimeType.Second ? localTime : localTime.withSecond(0);
    }

    @Override
    public void performValidation(@Nullable LocalTime value) throws ValidationException {
        if (value == null) {
            if (Boolean.TRUE.equals(getRequired())) {
                throw new RequiredValidationException(this);
            }
        }
    }

    @Nonnull
    public String toDisplayValue(@Nullable LocalTime value) {
        return value == null ? "Keine Angabe" : value
                .format(DateTimeFormatter.ofPattern(
                        TimeType.Second == mode ? "HH:mm:ss" : "HH:mm"
                )) + " Uhr";
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

        var timeA = formatValue(referencedValue);

        if (timeA == null) {
            return false;
        }

        if (!(comparedValue instanceof String sValB)) {
            return false;
        }

        var timeB = formatValue(sValB);
        if (timeB == null) {
            return false;
        }

        return switch (operator) {
            case Equals -> timeA.equals(timeB);
            case NotEquals -> !timeA.equals(timeB);
            case LessThan -> timeA.isBefore(timeB);
            case LessThanOrEqual -> timeA.compareTo(timeB) <= 0;
            case GreaterThan -> timeA.isAfter(timeB);
            case GreaterThanOrEqual -> timeA.compareTo(timeB) >= 0;
            default -> false;
        };
    }

    private static final Pattern localTimePattern = Pattern.compile("^(?:[01]\\d|2[0-3]):[0-5]\\d(?::[0-5]\\d)?$");

    @Nullable
    private static LocalTime parseLocalTime(@Nullable String value) {
        if (value == null) {
            return null;
        }

        if (!localTimePattern.matcher(value).matches()) {
            return null;
        }

        return LocalTime.parse(value);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TimeInputElement that = (TimeInputElement) o;
        return mode == that.mode;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(mode);
        return result;
    }

    @Nullable
    public TimeType getMode() {
        return mode;
    }

    public TimeInputElement setMode(@Nullable TimeType mode) {
        this.mode = mode;
        return this;
    }
}
