package de.aivot.GoverBackend.elements.models.form.input;

import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.DateType;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.form.models.FormState;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.elements.models.form.BaseInputElement;
import de.aivot.GoverBackend.models.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.models.pdf.ValuePdfRowDto;
import de.aivot.GoverBackend.utils.MapUtils;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class DateField extends BaseInputElement<String> {
    private static final Logger logger = LoggerFactory.getLogger(DateField.class);
    private final static ZoneId zoneId = ZoneId.of("Europe/Berlin");
    private String placeholder;
    private String autocomplete;
    private DateType mode;

    public DateField(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        super.applyValues(values);

        placeholder = MapUtils.getString(values, "placeholder");
        autocomplete = MapUtils.getString(values, "autocomplete");
        mode = MapUtils.getEnum(values, "mode", String.class, DateType.class, DateType.values());
    }

    @Override
    public String formatValue(Object value) {
        if (value instanceof String sValue) {
            return sValue;
        }
        return null;
    }

    @Override
    public void validate(String value) throws ValidationException {
        if (value == null && Boolean.TRUE.equals(getRequired())) {
            throw new RequiredValidationException(this);
        }

        if (value != null && getDate(value) == null) {
            throw new ValidationException(this, "Datum konnte nicht verarbeitet werden.");
        }
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(RootElement root, String value, String idPrefix, FormState formState) {
        List<BasePdfRowDto> rows = new LinkedList<>();

        String displayValue = "Keine Angabe";

        if (value != null) {
            String displayPattern = "dd.MM.yyyy";

            if (mode != null) {
                switch (mode) {
                    case Year -> displayPattern = "yyyy";
                    case Month -> displayPattern = "MM.yyyy";
                }
            }

            ZonedDateTime date = getDate(value);
            if (date != null) {
                displayValue = date
                        .format(
                                DateTimeFormatter
                                        .ofPattern(displayPattern)
                                        .withZone(zoneId)
                        );
            }
        }

        rows.add(new ValuePdfRowDto(
                getLabel(),
                displayValue,
                this
        ));

        return rows;
    }

    public String toDisplayValue(Object rawValue) {
        var value = formatValue(rawValue);

        String displayValue = "Keine Angabe";

        if (value != null) {
            String displayPattern = "dd.MM.yyyy";

            if (mode != null) {
                switch (mode) {
                    case Year -> displayPattern = "yyyy";
                    case Month -> displayPattern = "MM.yyyy";
                }
            }

            ZonedDateTime date = getDate(value);
            if (date != null) {
                displayValue = date
                        .format(
                                DateTimeFormatter
                                        .ofPattern(displayPattern)
                                        .withZone(zoneId)
                        );
            }
        }

        return displayValue;
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
            @Nullable
            ZonedDateTime dValA = getDate(sValA);
            if (dValA == null) {
                logger.warn("Could not parse date from string: " + sValA);
                return false;
            }

            ZonedDateTime today = ZonedDateTime.now(zoneId);

            switch (operator) {
                case YearsInPast -> {
                    int iValB;
                    try {
                        iValB = Integer.parseInt(sValB);
                    } catch (NumberFormatException ex) {
                        logger.error("Failed to parse int for years in past from string: " + sValB);
                        return false;
                    }
                    var target = today.minusYears(iValB);
                    logger.info("Comparing YearsInPast " + dValA + " with " + target + " and today: " + today);
                    return dValA.isBefore(target) || isSameDay(dValA, target);
                }
                case MonthsInPast -> {
                    int iValB;
                    try {
                        iValB = Integer.parseInt(sValB);
                    } catch (NumberFormatException ex) {
                        logger.error("Failed to parse int for months in past from string: " + sValB);
                        return false;
                    }
                    var target = today.minusMonths(iValB);
                    logger.info("Comparing MonthsInPast " + dValA + " with " + target + " and today: " + today);
                    return dValA.isBefore(target) || isSameDay(dValA, target);
                }
                case DaysInPast -> {
                    int iValB;
                    try {
                        iValB = Integer.parseInt(sValB);
                    } catch (NumberFormatException ex) {
                        logger.error("Failed to parse int for days in past from string: " + sValB);
                        return false;
                    }
                    var target = today.minusDays(iValB);
                    logger.info("Comparing DaysInPast " + dValA + " with " + target + " and today: " + today);
                    return dValA.isBefore(target) || isSameDay(dValA, target);
                }

                case YearsInFuture -> {
                    int iValB;
                    try {
                        iValB = Integer.parseInt(sValB);
                    } catch (NumberFormatException ex) {
                        return false;
                    }
                    var target = today.plusYears(iValB);
                    return dValA.isAfter(target) || isSameDay(dValA, target);
                }
                case MonthsInFuture -> {
                    int iValB;
                    try {
                        iValB = Integer.parseInt(sValB);
                    } catch (NumberFormatException ex) {
                        return false;
                    }
                    var target = today.plusMonths(iValB);
                    return dValA.isAfter(target) || isSameDay(dValA, target);
                }
                case DaysInFuture -> {
                    int iValB;
                    try {
                        iValB = Integer.parseInt(sValB);
                    } catch (NumberFormatException ex) {
                        return false;
                    }
                    var target = today.plusDays(iValB);
                    return dValA.isAfter(target) || isSameDay(dValA, target);
                }

                default -> {
                    ZonedDateTime dValB = getDate(sValB);

                    if (dValA == null || dValB == null) {
                        return false;
                    }

                    DateCompareResult res = new DateCompareResult(dValA, dValB);
                    DatePrecision prec = getPrecising(sValA);

                    return switch (operator) {
                        case Equals -> switch (prec) {
                            case iso, day -> res.dayEq() && res.monthEq() && res.yearEq();
                            case dayAnyMonthAnyYear -> res.dayEq();
                            case month -> res.monthEq() && res.yearEq();
                            case dayAndMonthAnyYear -> res.dayEq() && res.monthEq();
                            case year -> res.yearEq();
                        };
                        case NotEquals -> switch (prec) {
                            case iso, day -> !(res.dayEq() && res.monthEq() && res.yearEq());
                            case dayAnyMonthAnyYear -> !(res.dayEq());
                            case month -> !(res.monthEq() && res.yearEq());
                            case dayAndMonthAnyYear -> !(res.dayEq() && res.monthEq());
                            case year -> !res.yearEq();
                        };

                        case LessThan -> switch (prec) {
                            case iso, day -> res.yearLt() || res.yearEq() && res.monthLt() || res.yearEq() && res.monthEq() && res.dayLt();
                            case dayAnyMonthAnyYear -> res.dayLt();
                            case month -> res.yearLt() || res.yearEq() && res.monthLt();
                            case dayAndMonthAnyYear -> res.monthLt() || res.monthEq() && res.dayLt();
                            case year -> res.yearLt();
                        };
                        case LessThanOrEqual -> switch (prec) {
                            case iso, day -> res.yearLt() || res.yearEq() && res.monthLt() || res.yearEq() && res.monthEq() && (res.dayEq() || res.dayLt());
                            case dayAnyMonthAnyYear -> res.dayLt() || res.dayEq();
                            case month -> res.yearLt() || res.yearEq() && (res.monthEq() || res.monthLt());
                            case dayAndMonthAnyYear -> res.monthLt() || res.monthEq() && (res.dayEq() || res.dayLt());
                            case year -> res.yearEq() || res.yearLt();
                        };

                        case GreaterThan -> switch (prec) {
                            case iso, day -> res.yearGt() || res.yearEq() && res.monthGt() || res.yearEq() && res.monthEq() && res.dayGt();
                            case dayAnyMonthAnyYear -> res.dayGt();
                            case month -> res.yearGt() || res.yearEq() && res.monthGt();
                            case dayAndMonthAnyYear -> res.monthGt() || res.monthEq() && res.dayGt();
                            case year -> res.yearGt();
                        };
                        case GreaterThanOrEqual -> switch (prec) {
                            case iso, day -> res.yearGt() || res.yearEq() && res.monthGt() || res.yearEq() && res.monthEq() && (res.dayEq() || res.dayGt());
                            case dayAnyMonthAnyYear -> res.dayGt() || res.dayEq();
                            case month -> res.yearGt() || res.yearEq() && (res.monthEq() || res.monthGt());
                            case dayAndMonthAnyYear -> res.monthGt() || res.monthEq() && (res.dayEq() || res.dayGt());
                            case year -> res.yearEq() || res.yearGt();
                        };

                        default -> false;
                    };
                }
            }
        }

        return false;
    }

    private static final Pattern dayPattern = Pattern.compile("^\\d{2}\\.\\d{2}\\.\\d{4}$");
    private static final Pattern dayAnyMonthAnyYearPattern = Pattern.compile("^\\d\\d\\.$");
    private static final Pattern monthPattern = Pattern.compile("^\\d{2}\\.\\d{4}$");
    private static final Pattern monthAnyYearPattern = Pattern.compile("^\\d{2}\\.\\d{2}\\.$");
    private static final Pattern yearPattern = Pattern.compile("^\\d{4}$");

    private ZonedDateTime getDate(String value) {
        if (value == null) {
            return null;
        }

        try {
            return ZonedDateTime.parse(value);
        } catch (DateTimeException ex) {
            logger.error("Failed to parse date time from string: " + value, ex);

            try {
                var ld = LocalDate.parse(value);
                return ZonedDateTime.of(ld, ZonedDateTime.now().toLocalTime(), zoneId);
            } catch (DateTimeException ex1) {
                logger.error("Failed to parse date from string: " + value, ex1);

                try {
                    var ld2 = LocalDate.parse(value, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                    return ZonedDateTime.of(ld2, ZonedDateTime.now().toLocalTime(), zoneId);
                } catch (DateTimeException ex2) {
                    logger.error("Failed to parse date from string: " + value, ex2);

                    String preparedValue = null;

                    if (dayPattern.matcher(value).matches()) {
                        preparedValue = value;
                    } else if (dayAnyMonthAnyYearPattern.matcher(value).matches()) {
                        preparedValue = value + "01.2000";
                    } else if (monthPattern.matcher(value).matches()) {
                        preparedValue = "01." + value;
                    } else if (monthAnyYearPattern.matcher(value).matches()) {
                        preparedValue = value + "2000";
                    } else if (yearPattern.matcher(value).matches()) {
                        preparedValue = "01.01." + value;
                    }

                    if (preparedValue != null) {
                        try {
                            return ZonedDateTime.parse(preparedValue, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                        } catch (DateTimeParseException ex3) {
                            logger.error("Failed to parse prepared date from string: " + preparedValue, ex3);
                            return null;
                        }
                    }
                }
            }
        }

        logger.warn("Failed to parse prepared date from string: " + value);

        return null;
    }

    private DatePrecision getPrecising(String value) {
        if (dayPattern.matcher(value).matches()) {
            return DatePrecision.day;
        } else if (dayAnyMonthAnyYearPattern.matcher(value).matches()) {
            return DatePrecision.dayAnyMonthAnyYear;
        } else if (monthPattern.matcher(value).matches()) {
            return DatePrecision.month;
        } else if (monthAnyYearPattern.matcher(value).matches()) {
            return DatePrecision.dayAndMonthAnyYear;
        } else if (yearPattern.matcher(value).matches()) {
            return DatePrecision.year;
        }
        return DatePrecision.iso;
    }

    private enum DatePrecision {
        day,
        dayAnyMonthAnyYear,
        month,
        dayAndMonthAnyYear,
        year,
        iso,
    }

    private static class DateCompareResult {
        private final int dayRes;
        private final int monthRes;
        private final int yearRes;

        public DateCompareResult(ZonedDateTime d1, ZonedDateTime d2) {
            dayRes = Integer.compare(d1.withZoneSameInstant(zoneId).getDayOfMonth(), d2.withZoneSameInstant(zoneId).getDayOfMonth());
            monthRes = Integer.compare(d1.withZoneSameInstant(zoneId).getMonthValue(), d2.withZoneSameInstant(zoneId).getMonthValue());
            yearRes = Integer.compare(d1.withZoneSameInstant(zoneId).getYear(), d2.withZoneSameInstant(zoneId).getYear());
        }

        public boolean dayLt() {
            return dayRes < 0;
        }

        public boolean dayEq() {
            return dayRes == 0;
        }

        public boolean dayGt() {
            return dayRes > 0;
        }

        public boolean monthLt() {
            return monthRes < 0;
        }

        public boolean monthEq() {
            return monthRes == 0;
        }

        public boolean monthGt() {
            return monthRes > 0;
        }

        public boolean yearLt() {
            return yearRes < 0;
        }

        public boolean yearEq() {
            return yearRes == 0;
        }

        public boolean yearGt() {
            return yearRes > 0;
        }
    }

    private boolean isSameDay(ZonedDateTime d1, ZonedDateTime d2) {
        var d1Local = d1.withZoneSameInstant(zoneId);
        var d2Local = d2.withZoneSameInstant(zoneId);

        return (
                d1Local.getYear() == d2Local.getYear() &&
                d1Local.getMonth() == d2Local.getMonth() &&
                d1Local.getDayOfMonth() == d2Local.getDayOfMonth()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        DateField dateField = (DateField) o;

        if (!Objects.equals(placeholder, dateField.placeholder))
            return false;
        if (!Objects.equals(autocomplete, dateField.autocomplete))
            return false;
        return mode == dateField.mode;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (placeholder != null ? placeholder.hashCode() : 0);
        result = 31 * result + (autocomplete != null ? autocomplete.hashCode() : 0);
        result = 31 * result + (mode != null ? mode.hashCode() : 0);
        return result;
    }

    //region Getter & Setter

    @Nullable
    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    @Nullable
    public String getAutocomplete() {
        return autocomplete;
    }

    public void setAutocomplete(String autocomplete) {
        this.autocomplete = autocomplete;
    }

    @Nullable
    public DateType getMode() {
        return mode;
    }

    public void setMode(DateType mode) {
        this.mode = mode;
    }

    //endregion

}
