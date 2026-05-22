package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.PrintableElement;
import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.DateType;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.utils.ApplicationTimeZone;
import de.aivot.GoverBackend.utils.IsoTimestampUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.regex.Pattern;

public class DateInputElement extends BaseInputElement<ZonedDateTime> implements PrintableElement<ZonedDateTime> {
    private static final Logger logger = LoggerFactory.getLogger(DateInputElement.class);

    @Nullable
    private String placeholder;

    @Nullable
    private String autocomplete;

    @Nullable
    private DateType mode;

    public DateInputElement() {
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
                                            .withZone(ApplicationTimeZone.getZoneId())
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

        ZonedDateTime dValA = _formatValue(referencedValue);

        if (dValA == null) {
            logger.warn("Could not parse date from string: " + referencedValue.toString());
            return false;
        }

        String sValB;
        if (comparedValue instanceof String zValue) {
            sValB = zValue;
        } else {
            sValB = null;
        }

        if (sValB == null) {
            return false;
        }

        ZonedDateTime today = ZonedDateTime.now(ApplicationTimeZone.getZoneId());

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
                DatePrecision prec = getPrecising(sValB);

                ZonedDateTime dValB;
                switch (prec) {
                    case dayAnyMonthAnyYear -> {
                        dValB = _formatValue(sValB + "01.2000");
                    }
                    case month -> {
                        dValB = _formatValue("01." + sValB);
                    }
                    case dayAndMonthAnyYear -> {
                        dValB = _formatValue(sValB + "2000");
                    }
                    case year -> {
                        dValB = _formatValue("01.01." + sValB);
                    }
                    case day,iso -> {
                        dValB = _formatValue(sValB);
                    }
                    default ->  dValB = null;
                }

                if (dValB == null) {
                    return false;
                }

                DateCompareResult res = new DateCompareResult(dValA, dValB);

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
            dayRes = Integer.compare(d1.withZoneSameInstant(ApplicationTimeZone.getZoneId()).getDayOfMonth(), d2.withZoneSameInstant(ApplicationTimeZone.getZoneId()).getDayOfMonth());
            monthRes = Integer.compare(d1.withZoneSameInstant(ApplicationTimeZone.getZoneId()).getMonthValue(), d2.withZoneSameInstant(ApplicationTimeZone.getZoneId()).getMonthValue());
            yearRes = Integer.compare(d1.withZoneSameInstant(ApplicationTimeZone.getZoneId()).getYear(), d2.withZoneSameInstant(ApplicationTimeZone.getZoneId()).getYear());
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
        var d1Local = d1.withZoneSameInstant(ApplicationTimeZone.getZoneId());
        var d2Local = d2.withZoneSameInstant(ApplicationTimeZone.getZoneId());

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
            case LocalDate ldValue -> ldValue.atStartOfDay(ApplicationTimeZone.getZoneId());
            case LocalTime lValue -> ZonedDateTime.of(LocalDate.now(ApplicationTimeZone.getZoneId()), lValue, ApplicationTimeZone.getZoneId());
            case Instant iValue -> iValue.atZone(ApplicationTimeZone.getZoneId());
            case String sValue -> {
                try {
                    // UI date pickers submit UTC ISO strings. Convert them back into the office
                    // timezone only for display and local rule evaluation.
                    yield IsoTimestampUtils.parseIsoTimestamp(sValue, ApplicationTimeZone.getZoneId()).atZone(ApplicationTimeZone.getZoneId());
                } catch (DateTimeException ex) {
                    try {
                        var ld = LocalDate.parse(sValue);
                        yield ld.atStartOfDay(ApplicationTimeZone.getZoneId());
                    } catch (DateTimeException ex1) {
                        try {
                            var ld2 = LocalDate.parse(sValue, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                            yield ld2.atStartOfDay(ApplicationTimeZone.getZoneId());
                        } catch (DateTimeException ex2) {
                            String preparedValue = null;

                            if (dayPattern.matcher(sValue).matches()) {
                                preparedValue = sValue;
                            } else if (dayAnyMonthAnyYearPattern.matcher(sValue).matches()) {
                                preparedValue = sValue + "01.2000";
                            } else if (monthPattern.matcher(sValue).matches()) {
                                preparedValue = "01." + sValue;
                            } else if (monthAnyYearPattern.matcher(sValue).matches()) {
                                preparedValue = sValue + "2000";
                            } else if (yearPattern.matcher(sValue).matches()) {
                                preparedValue = "01.01." + sValue;
                            }

                            if (preparedValue != null) {
                                try {
                                    yield LocalDate
                                            .parse(preparedValue, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                                            .atStartOfDay(ApplicationTimeZone.getZoneId());
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

        DateInputElement dateField = (DateInputElement) o;
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

    public DateInputElement setPlaceholder(@Nullable String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    @Nullable
    public String getAutocomplete() {
        return autocomplete;
    }

    public DateInputElement setAutocomplete(@Nullable String autocomplete) {
        this.autocomplete = autocomplete;
        return this;
    }

    @Nullable
    public DateType getMode() {
        return mode;
    }

    public DateInputElement setMode(@Nullable DateType mode) {
        this.mode = mode;
        return this;
    }

    //endregion
}
