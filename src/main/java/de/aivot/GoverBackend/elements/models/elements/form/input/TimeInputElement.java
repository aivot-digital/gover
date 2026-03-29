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
import java.util.regex.Pattern;

public class TimeInputElement extends BaseInputElement<ZonedDateTime> implements PrintableElement<ZonedDateTime> {
    private final static ZoneId zoneId = ZoneId.of("Europe/Paris");
    @Nullable
    private TimeType mode;

    public TimeInputElement() {
        super(ElementType.Time);
    }

    @Override
    public ZonedDateTime formatValue(Object value) {
        return DateInputElement._formatValue(value);
    }

    @Override
    public void performValidation(@Nullable ZonedDateTime value) throws ValidationException {
        if (value == null) {
            if (Boolean.TRUE.equals(getRequired())) {
                throw new RequiredValidationException(this);
            }
        }
    }

    @Nonnull
    public String toDisplayValue(@Nullable ZonedDateTime value) {
        return value == null ? "Keine Angabe" : value
                                                        .format(DateTimeFormatter
                                                                .ofPattern(TimeType.Second == mode ? "HH:mm:ss" : "HH:mm")
                                                                .withZone(zoneId)) + " Uhr";
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

        ZonedDateTime dValA;
        switch (referencedValue) {
            case ZonedDateTime zValue -> dValA = zValue;
            case LocalDateTime lValue -> dValA = lValue.atZone(zoneId);
            case LocalDate ldValue -> dValA = ZonedDateTime.of(ldValue, LocalTime.now(), zoneId);
            case LocalTime lValue -> dValA = ZonedDateTime.of(LocalDate.now(), lValue, zoneId);
            case Instant iValue -> dValA = iValue.atZone(zoneId);
            case String sValue -> dValA = formatValue(sValue);
            default -> dValA = null;
        }

        if (dValA == null) {
            return false;
        }

        String sValA = dValA.format(
                DateTimeFormatter
                        .ofPattern(TimeType.Second == mode ? "HH:mm:ss" : "HH:mm")
                        .withZone(zoneId)
        );

        if (!(comparedValue instanceof String)) {
            return false;
        }

        String sValB = (String) comparedValue;

        Integer hourA = getHour(sValA);
        Integer minuteA = getMinute(sValA);
        Integer secondA = getSecond(sValA);

        Integer hourB = getHour(sValB);
        Integer minuteB = getMinute(sValB);
        Integer secondB = getSecond(sValB);

        if (hourA == null || minuteA == null || secondA == null || hourB == null || minuteB == null || secondB == null) {
            return false;
        }

        final boolean hourEquals = hourA.equals(hourB);
        final boolean minuteEquals = minuteA.equals(minuteB);
        final boolean secondEquals = secondA.equals(secondB);
        final boolean compareSeconds = TimeType.Second == mode;
        final boolean equals = compareSeconds ? (hourEquals && minuteEquals && secondEquals) : (hourEquals && minuteEquals);

        return switch (operator) {
            case Equals -> equals;
            case NotEquals -> !(equals);

            case LessThan -> compareSeconds
                    ? hourA.compareTo(hourB) < 0 || (hourEquals && minuteA.compareTo(minuteB) < 0) || (hourEquals && minuteEquals && secondA.compareTo(secondB) < 0)
                    : hourA.compareTo(hourB) < 0 || (hourEquals && minuteA.compareTo(minuteB) < 0);
            case LessThanOrEqual -> compareSeconds
                    ? hourA.compareTo(hourB) <= 0 || (hourEquals && minuteA.compareTo(minuteB) <= 0) || (hourEquals && minuteEquals && secondA.compareTo(secondB) <= 0)
                    : hourA.compareTo(hourB) <= 0 || (hourEquals && minuteA.compareTo(minuteB) <= 0);

            case GreaterThan -> compareSeconds
                    ? hourA.compareTo(hourB) > 0 || (hourEquals && minuteA.compareTo(minuteB) > 0) || (hourEquals && minuteEquals && secondA.compareTo(secondB) > 0)
                    : hourA.compareTo(hourB) > 0 || (hourEquals && minuteA.compareTo(minuteB) > 0);
            case GreaterThanOrEqual -> compareSeconds
                    ? hourA.compareTo(hourB) >= 0 || (hourEquals && minuteA.compareTo(minuteB) >= 0) || (hourEquals && minuteEquals && secondA.compareTo(secondB) >= 0)
                    : hourA.compareTo(hourB) >= 0 || (hourEquals && minuteA.compareTo(minuteB) >= 0);

            default -> false;
        };
    }

    private static final Pattern hhMmPattern = Pattern.compile("^\\d\\d:\\d\\d$");
    private static final Pattern hhMmSsPattern = Pattern.compile("^\\d\\d:\\d\\d:\\d\\d$");

    private Integer getHour(String value) {
        if (hhMmPattern.matcher(value).matches() || hhMmSsPattern.matcher(value).matches()) {
            String[] parts = value.split(":");
            return Integer.parseInt(parts[0]);
        } else {
            ZonedDateTime d = parseIsoDate(value);
            if (d != null) {
                return d.withZoneSameInstant(zoneId).getHour();
            }
            return null;
        }
    }

    private Integer getMinute(String value) {
        if (hhMmPattern.matcher(value).matches() || hhMmSsPattern.matcher(value).matches()) {
            String[] parts = value.split(":");
            return Integer.parseInt(parts[1]);
        } else {
            ZonedDateTime d = parseIsoDate(value);
            if (d != null) {
                return d.withZoneSameInstant(zoneId).getMinute();
            }
            return null;
        }
    }

    private Integer getSecond(String value) {
        if (hhMmPattern.matcher(value).matches()) {
            return 0;
        }

        if (hhMmSsPattern.matcher(value).matches()) {
            String[] parts = value.split(":");
            return Integer.parseInt(parts[2]);
        }

        ZonedDateTime d = parseIsoDate(value);
        if (d != null) {
            return d.withZoneSameInstant(zoneId).getSecond();
        }
        return null;
    }

    private ZonedDateTime parseIsoDate(String value) {
        if (value == null) {
            return null;
        }

        try {
            return ZonedDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException ex) {
            return null;
        }
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
