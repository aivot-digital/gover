package de.aivot.GoverBackend.models.elements.form.input;

import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.models.elements.form.BaseInputElement;
import de.aivot.GoverBackend.models.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.models.pdf.ValuePdfRowDto;

import javax.script.ScriptEngine;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TimeField extends BaseInputElement<String> {
    private final static ZoneId zoneId = ZoneId.of("Europe/Paris");

    public TimeField(Map<String, Object> data) {
        super(data);
    }

    @Override
    public String formatValue(Object value) {
        if (value instanceof String sValue) {
            return sValue;
        }
        return null;
    }

    @Override
    public void validate(String idPrefix, RootElement root, Map<String, Object> customerInput, String value, ScriptEngine scriptEngine) throws ValidationException {
        if (value == null) {
            if (Boolean.TRUE.equals(getRequired())) {
                throw new RequiredValidationException(this);
            }
        } else {
            if (parseIsoDate(value) == null) {
                throw new ValidationException(this, "Failed to parse time:" + value);
            }
        }
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(RootElement root, Map<String, Object> customerInput, String value, String idPrefix, ScriptEngine scriptEngine) {
        List<BasePdfRowDto> fields = new LinkedList<>();

        ZonedDateTime time = parseIsoDate(value);

        String valueText = time == null ? "Keine Angabe" : time.format(DateTimeFormatter
                .ofPattern("HH:mm")
                .withZone(zoneId)) + " Uhr";

        fields.add(new ValuePdfRowDto(
                getLabel(),
                valueText,
                this
        ));

        return fields;
    }

    public String toDisplayValue(Object rawValue) {
        var value = formatValue(rawValue);

        ZonedDateTime time;
        try {
            time  = parseIsoDate(value);
        } catch (Exception ex) {
            return "Keine Angabe";
        }

        return time == null ? "Keine Angabe" : time.format(DateTimeFormatter
                .ofPattern("HH:mm")
                .withZone(zoneId)) + " Uhr";
    }

    @Override
    public boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
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

        if (referencedValue instanceof String sValA && comparedValue instanceof String sValB) {
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
                case GreaterThanOrEqual ->
                        hourA.compareTo(hourB) >= 0 || (hourEquals && minuteA.compareTo(minuteB) >= 0);

                default -> false;
            };
        }

        return false;
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
