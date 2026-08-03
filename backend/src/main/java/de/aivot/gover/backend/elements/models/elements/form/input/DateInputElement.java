package de.aivot.gover.backend.elements.models.elements.form.input;

import de.aivot.gover.backend.elements.models.elements.BaseInputElement;
import de.aivot.gover.backend.elements.models.elements.PrintableElement;
import de.aivot.gover.backend.enums.ConditionOperator;
import de.aivot.gover.backend.enums.DateType;
import de.aivot.gover.backend.enums.ElementType;
import de.aivot.gover.backend.exceptions.RequiredValidationException;
import de.aivot.gover.backend.exceptions.ValidationException;
import de.aivot.gover.backend.utils.ApplicationTimeZone;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Objects;
import java.util.regex.Pattern;

public class DateInputElement extends BaseInputElement<TemporalAccessor> implements PrintableElement<TemporalAccessor> {
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
    public TemporalAccessor formatValue(@Nullable Object value) {
        // Preserve precision in the runtime type itself. This lets every Jackson boundary
        // serialize the value correctly without needing the originating element definition.
        return normalizePrecision(_formatValue(value), mode);
    }

    @Override
    public void performValidation(@Nullable TemporalAccessor value) throws ValidationException {
        if (value == null && Boolean.TRUE.equals(getRequired())) {
            throw new RequiredValidationException(this);
        }
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable TemporalAccessor value) {
        if (value == null) {
            return "Keine Angabe";
        }

        return switch (value) {
            case Year year -> year.toString();
            case YearMonth yearMonth -> yearMonth.format(DateTimeFormatter.ofPattern("MM.yyyy"));
            case LocalDate localDate -> localDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            default -> "Keine Angabe";
        };
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

        var dValA = toLocalDate(formatValue(referencedValue));

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

        var today = LocalDate.now(ApplicationTimeZone.getZoneId());

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
                return !dValA.isAfter(target);
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
                return !dValA.isAfter(target);
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
                return !dValA.isAfter(target);
            }

            case YearsInFuture -> {
                int iValB;
                try {
                    iValB = Integer.parseInt(sValB);
                } catch (NumberFormatException ex) {
                    return false;
                }
                var target = today.plusYears(iValB);
                return !dValA.isBefore(target);
            }
            case MonthsInFuture -> {
                int iValB;
                try {
                    iValB = Integer.parseInt(sValB);
                } catch (NumberFormatException ex) {
                    return false;
                }
                var target = today.plusMonths(iValB);
                return !dValA.isBefore(target);
            }
            case DaysInFuture -> {
                int iValB;
                try {
                    iValB = Integer.parseInt(sValB);
                } catch (NumberFormatException ex) {
                    return false;
                }
                var target = today.plusDays(iValB);
                return !dValA.isBefore(target);
            }

            default -> {
                DatePrecision prec = getPrecision(sValB);
                var dValB = parseComparisonDateString(sValB, prec);

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
    private static final Pattern isoMonthPattern = Pattern.compile("^\\d{4}-\\d{2}$");

    private DatePrecision getPrecision(String value) {
        if (dayPattern.matcher(value).matches()) {
            return DatePrecision.day;
        } else if (dayAnyMonthAnyYearPattern.matcher(value).matches()) {
            return DatePrecision.dayAnyMonthAnyYear;
        } else if (monthPattern.matcher(value).matches()) {
            return DatePrecision.month;
        } else if (isoMonthPattern.matcher(value).matches()) {
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

        public DateCompareResult(LocalDate d1, LocalDate d2) {
            dayRes = Integer.compare(d1.getDayOfMonth(), d2.getDayOfMonth());
            monthRes = Integer.compare(d1.getMonthValue(), d2.getMonthValue());
            yearRes = Integer.compare(d1.getYear(), d2.getYear());
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

    @Nullable
    public static TemporalAccessor _formatValue(@Nullable Object value) {
        return switch (value) {
            case null -> null;
            case LocalDate localDate -> localDate;
            case YearMonth yearMonth -> yearMonth;
            case Year year -> year;
            // Java temporal objects may originate from internal computations or legacy
            // effective values. Interpret their instant in the application timezone,
            // while external strings remain restricted to zone-free calendar formats.
            case ZonedDateTime zonedDateTime -> zonedDateTime
                    .withZoneSameInstant(ApplicationTimeZone.getZoneId())
                    .toLocalDate();
            case OffsetDateTime offsetDateTime -> offsetDateTime
                    .atZoneSameInstant(ApplicationTimeZone.getZoneId())
                    .toLocalDate();
            case Instant instant -> instant
                    .atZone(ApplicationTimeZone.getZoneId())
                    .toLocalDate();
            case String sValue -> parseDateString(sValue);
            default -> null;
        };
    }

    @Nullable
    private static TemporalAccessor parseDateString(@Nonnull String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ignored) {
        }

        try {
            if (isoMonthPattern.matcher(value).matches()) {
                return YearMonth.parse(value);
            }
            if (yearPattern.matcher(value).matches()) {
                return Year.parse(value);
            }
        } catch (DateTimeParseException ignored) {
            return null;
        }

        return null;
    }

    @Nullable
    private static LocalDate parseComparisonDateString(
            @Nonnull String value,
            @Nonnull DatePrecision precision
    ) {
        // Runtime values use canonical ISO formats. German full and partial formats
        // remain accepted only for authored condition values stored in form definitions.
        var preparedValue = switch (precision) {
            case day -> value;
            case dayAnyMonthAnyYear -> value + "01.2000";
            case month -> isoMonthPattern.matcher(value).matches() ? null : "01." + value;
            case dayAndMonthAnyYear -> value + "2000";
            case year, iso -> null;
        };

        if (preparedValue == null) {
            return toLocalDate(parseDateString(value));
        }

        try {
            return LocalDate
                    .parse(preparedValue, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    @Nullable
    static TemporalAccessor normalizePrecision(
            @Nullable TemporalAccessor value,
            @Nullable DateType mode
    ) {
        var localDate = toLocalDate(value);
        if (localDate == null) {
            return null;
        }

        return switch (mode == null ? DateType.Day : mode) {
            case Day -> localDate;
            case Month -> YearMonth.from(localDate);
            case Year -> Year.from(localDate);
        };
    }

    @Nullable
    static LocalDate toLocalDate(@Nullable TemporalAccessor value) {
        return switch (value) {
            case null -> null;
            case LocalDate localDate -> localDate;
            case YearMonth yearMonth -> yearMonth.atDay(1);
            case Year year -> year.atDay(1);
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
