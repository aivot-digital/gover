package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public class TimeField extends BaseInputElement<ZonedDateTime> {
    private final static ZoneId zoneId = ZoneId.of("Europe/Paris");

    public TimeField() {
        super(ElementType.Time);
    }

    @Override
    public ZonedDateTime formatValue(Object value) {
        return DateField._formatValue(value);
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
                                                                .ofPattern("HH:mm")
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
                        .ofPattern("HH:mm")
                        .withZone(zoneId)
        );

        if (!(comparedValue instanceof String)) {
            return false;
        }

        String sValB = (String) comparedValue;

        Integer hourA = getHour(sValA);
        Integer minuteA = getMinute(sValA);

        Integer hourB = getHour(sValB);
        Integer minuteB = getMinute(sValB);

        if (hourA == null || minuteA == null || hourB == null || minuteB == null) {
            return false;
        }

        final boolean hourEquals = hourA.equals(hourB);
        final boolean minuteEquals = minuteA.equals(minuteB);
        final boolean equals = hourEquals && minuteEquals;

        return switch (operator) {
            case Equals -> equals;
            case NotEquals -> !(equals);

            case LessThan -> hourA.compareTo(hourB) < 0 || (hourEquals && minuteA.compareTo(minuteB) < 0);
            case LessThanOrEqual -> hourA.compareTo(hourB) <= 0 || (hourEquals && minuteA.compareTo(minuteB) <= 0);

            case GreaterThan -> hourA.compareTo(hourB) > 0 || (hourEquals && minuteA.compareTo(minuteB) > 0);
            case GreaterThanOrEqual -> hourA.compareTo(hourB) >= 0 || (hourEquals && minuteA.compareTo(minuteB) >= 0);

            default -> false;
        };
    }

    private static final Pattern hhMmPattern = Pattern.compile("^\\d\\d:\\d\\d$");

    private Integer getHour(String value) {
        if (hhMmPattern.matcher(value).matches()) {
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
        if (hhMmPattern.matcher(value).matches()) {
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
}
