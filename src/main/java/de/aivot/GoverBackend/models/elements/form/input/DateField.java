package de.aivot.GoverBackend.models.elements.form.input;

import com.sun.istack.Nullable;
import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.DateType;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.models.elements.form.BaseInputElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.pdf.ValuePdfRowDto;
import de.aivot.GoverBackend.utils.MapUtils;
import net.bytebuddy.asm.Advice;

import javax.script.ScriptEngine;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class DateField extends BaseInputElement<String> {
    private final static ZoneId zoneId = ZoneId.of("Europe/Paris");
    private String placeholder;
    private DateType mode;
    private Boolean mustBePast;
    private Boolean mustBeFuture;

    public DateField(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        super.applyValues(values);

        placeholder = MapUtils.getString(values, "placeholder");
        mode = MapUtils.getEnum(values, "mode", String.class, DateType.values());
        mustBePast = MapUtils.getBoolean(values, "mustBePast");
        mustBeFuture = MapUtils.getBoolean(values, "mustBeFuture");
    }

    @Override
    protected String formatValue(Object value) {
        if (value instanceof String sValue) {
            return sValue;
        }
        return null;
    }

    @Override
    public void validate(String idPrefix, RootElement root, Map<String, Object> customerInput, String value, ScriptEngine scriptEngine) throws ValidationException {
        if (value == null && Boolean.TRUE.equals(getRequired())) {
            throw new RequiredValidationException(this);
        }

        if (value != null) {
            LocalDate date;
            try {
                var cleandDate = value;
                if (value.contains("T")) {
                    cleandDate = value.split("T")[0];
                }
                date = LocalDate.parse(cleandDate);
            } catch (DateTimeParseException e) {
                throw new ValidationException(this, "Failed to parse date:" + e.getMessage());
            }
            validateIsFuture(date);
            validateIsPast(date);
        }
    }

    private void validateIsFuture(LocalDate date) throws ValidationException {
        if (Boolean.TRUE.equals(mustBeFuture)) {
            if (!date.isAfter(LocalDate.now())) {
                throw new ValidationException(this, "Must be future");
            }
        }
    }

    private void validateIsPast(LocalDate date) throws ValidationException {
        if (Boolean.TRUE.equals(mustBePast)) {
            if (!date.isBefore(LocalDate.now())) {
                throw new ValidationException(this, "Must be past");
            }
        }
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(RootElement root, Map<String, Object> customerInput, String value, String idPrefix, ScriptEngine scriptEngine) {
        List<BasePdfRowDto> rows = new LinkedList<>();

        String displayValue = "Keine Angaben";

        if (value != null) {
            String displayPattern = "dd.MM.yyyy";

            if (mode != null) {
                switch (mode) {
                    case Year -> displayPattern = "yyyy";
                    case Month -> displayPattern = "mm.yyyy";
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
                displayValue
        ));

        return rows;
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
            ZonedDateTime dValA = getDate(sValA);
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
                } catch (DateTimeParseException ex2) {
                    return null;
                }
            }
        }

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

    //region Getter & Setter

    @Nullable
    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    @Nullable
    public DateType getMode() {
        return mode;
    }

    public void setMode(DateType mode) {
        this.mode = mode;
    }

    @Nullable
    public Boolean getMustBePast() {
        return mustBePast;
    }

    public void setMustBePast(Boolean mustBePast) {
        this.mustBePast = mustBePast;
    }

    @Nullable
    public Boolean getMustBeFuture() {
        return mustBeFuture;
    }

    public void setMustBeFuture(Boolean mustBeFuture) {
        this.mustBeFuture = mustBeFuture;
    }

    //endregion

}
