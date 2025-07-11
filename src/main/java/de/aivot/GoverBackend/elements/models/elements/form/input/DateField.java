package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.DateType;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.regex.Pattern;

public class DateField extends BaseInputElement<ZonedDateTime> {
    private static final Logger logger = LoggerFactory.getLogger(DateField.class);
    private final static ZoneId zoneId = ZoneId.of("Europe/Berlin");

    @Nullable
    private String placeholder;
    @Nullable
    private String autocomplete;
    @Nullable
    private DateType mode;

    public DateField() {
        super(ElementType.Date);
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
        String displayValue = "Keine Angabe";

        if (value != null) {
            String displayPattern = "dd.MM.yyyy";

            if (mode != null) {
                switch (mode) {
                    case Year -> displayPattern = "yyyy";
                    case Month -> displayPattern = "MM.yyyy";
                }
            }

            displayValue = value
                    .format(
                            DateTimeFormatter
                                    .ofPattern(displayPattern)
                                    .withZone(zoneId)
                    );
        }

        return displayValue;
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

        if (referencedValue instanceof String sValA && comparedValue instanceof String sValB) {
            @Nullable
            ZonedDateTime dValA = _formatValue(sValA);
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
                    ZonedDateTime dValB = _formatValue(sValB);

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
                        var ld = LocalDate.parse(sValue);
                        yield ZonedDateTime.of(ld, ZonedDateTime.now().toLocalTime(), zoneId);
                    } catch (DateTimeException ex1) {
                        try {
                            var ld2 = LocalDate.parse(sValue, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                            yield ZonedDateTime.of(ld2, ZonedDateTime.now().toLocalTime(), zoneId);
                        } catch (DateTimeException ex2) {
                            String preparedValue = null;

                            if (dayPattern.matcher(sValue).matches()) {
                                preparedValue = sValue;
                            } else if (dayAnyMonthAnyYearPattern.matcher(sValue).matches()) {
                                preparedValue = value + "01.2000";
                            } else if (monthPattern.matcher(sValue).matches()) {
                                preparedValue = "01." + value;
                            } else if (monthAnyYearPattern.matcher(sValue).matches()) {
                                preparedValue = value + "2000";
                            } else if (yearPattern.matcher(sValue).matches()) {
                                preparedValue = "01.01." + value;
                            }

                            if (preparedValue != null) {
                                try {
                                    yield ZonedDateTime.parse(preparedValue, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                                } catch (DateTimeParseException ex3) {
                                    yield null;
                                }
                            } else {
                                yield null;
                            }
                        }
                    }
                }
            }
            default -> null;
        };
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        DateField dateField = (DateField) o;
        return Objects.equals(placeholder, dateField.placeholder) && Objects.equals(autocomplete, dateField.autocomplete) && mode == dateField.mode;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(placeholder);
        result = 31 * result + Objects.hashCode(autocomplete);
        result = 31 * result + Objects.hashCode(mode);
        return result;
    }

    // endregion

    //region Getter & Setter

    @Nullable
    public String getPlaceholder() {
        return placeholder;
    }

    public DateField setPlaceholder(@Nullable String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    @Nullable
    public String getAutocomplete() {
        return autocomplete;
    }

    public DateField setAutocomplete(@Nullable String autocomplete) {
        this.autocomplete = autocomplete;
        return this;
    }

    @Nullable
    public DateType getMode() {
        return mode;
    }

    public DateField setMode(@Nullable DateType mode) {
        this.mode = mode;
        return this;
    }

    //endregion
}
