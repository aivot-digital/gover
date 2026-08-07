package de.aivot.prosuna.backend.nocode.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.prosuna.backend.core.services.ObjectMapperFactory;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.prosuna.backend.utils.ApplicationTimeZone;
import de.aivot.prosuna.backend.utils.IsoTimestampUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an operator in the NoCode language.
 */
public abstract class NoCodeOperator {
    private static final DateTimeFormatter LOCAL_TIME_SECONDS_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Returns the identifier of the operator.
     * This is used to reference the operator in the NoCode language.
     * The reference always consists of the package name where the operator is located and the identifier.
     *
     * @return the identifier of the operator
     */
    public abstract String getIdentifier();

    /**
     * Returns the label of the operator.
     * The label is used to display the operator in the UI.
     *
     * @return the label of the operator
     */
    public abstract String getLabel();

    /**
     * Returns the abstract of the operator.
     *
     * @return the abstract of the operator
     */
    public abstract String getAbstract();

    /**
     * Returns the description of the operator.
     * The description is used to provide additional information about the operator.
     *
     * @return the description of the operator
     */
    public abstract String getDescription();

    /**
     * Returns the tags of the operator.
     * Tags are used to categorize the operator and can be used for filtering in the UI.
     * If no tags are defined, an empty array is returned.
     *
     * @return the tags of the operator
     */
    public String[] getTags() {
        return new String[0];
    }

    /**
     * Returns a human-readable template of the operator.
     * This template will later be filled with the parameters of the operator to provide a better understanding of how the operator works.
     * The template contains placeholders for the parameters of the operator.
     * For example: "Add #0 to #1" where #0 and #1 are placeholders for the parameters.
     *
     * @return a human-readable template of the operator
     */
    @Nullable
    public String getHumanReadableTemplate() {
        return null;
    }

    /**
     * Returns the list of parameters that the operator expects.
     *
     * @return the array of parameters that the operator expects
     */
    public abstract NoCodeSignatur[] getSignatures();

    @Nonnull
    public NoCodeResult evaluate(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        if (data == null) {
            throw new NullPointerException("Data is null. Needs to be at least an empty map");
        }

        if (args == null) {
            throw new NullPointerException("Arguments are null. Needs to be at least an empty array");
        }

        var signatures = getSignatures();
        if (signatures == null || signatures.length == 0) {
            throw new NoCodeWrongArgumentCountException(0, args.length);
        }

        var actualParametersLength = args.length;
        var someMatch = false;
        var minimumExpectedParametersLength = Integer.MAX_VALUE;
        for (var signature : signatures) {
            var expectedParametersLength = signature.parameters().length;
            minimumExpectedParametersLength = Math.min(minimumExpectedParametersLength, expectedParametersLength);
            if (actualParametersLength == expectedParametersLength) {
                someMatch = true;
                break;
            }

            if (supportsVariableArgumentCount() && actualParametersLength > expectedParametersLength) {
                someMatch = true;
                break;
            }
        }


        if (!someMatch) {
            throw new NoCodeWrongArgumentCountException(minimumExpectedParametersLength, actualParametersLength);
        }

        return performEvaluation(data, args);
    }

    /**
     * Evaluates the operator with the given arguments.
     * The arguments are passed as an array of objects.
     * The amount and types of the arguments must be checked during the evaluation.
     * Arguments are <strong>not</strong> limited to the specified types.
     * The result of the evaluation is returned as a {@link NoCodeResult}.
     * Please make sure, that the evaluate method makes up for type mismatches like receiving a string instead of a number, and it can handle these.
     *
     * @param data the data that is used existent in the context.
     * @param args the arguments that are passed to the operator.
     * @return the result of the evaluation.
     * @throws NoCodeException if an error occurs during the evaluation.
     */
    protected abstract NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException;

    /**
     * Returns the message that should be displayed when the operator is deprecated.
     * If the operator is not deprecated, this method should return null.
     *
     * @return the message that should be displayed when the operator is deprecated.
     */
    public String getDeprecatedMessage() {
        return null;
    }

    protected boolean supportsVariableArgumentCount() {
        return false;
    }

    @Nullable
    public Object castToTypeOfReference(@Nullable Object referenceObject, @Nullable Object objectToCast) {
        if (referenceObject == null || objectToCast == null) {
            return null;
        }

        switch (referenceObject) {
            case String sReferenceObject -> {
                return castToString(objectToCast);
            }
            case Integer iReferenceObject -> {
                return castToNumber(objectToCast).intValue();
            }
            case Float fReferenceObject -> {
                return castToNumber(objectToCast).floatValue();
            }
            case Double dReferenceObject -> {
                return castToNumber(objectToCast).doubleValue();
            }
            case Long lReferenceObject -> {
                return castToNumber(objectToCast).longValue();
            }
            case BigDecimal bdReferenceObject -> {
                return castToNumber(objectToCast);
            }
            case Boolean bReferenceObject -> {
                return castToBoolean(objectToCast);
            }
            case List<?> lReferenceObject -> {
                return castToList(objectToCast);
            }
            case Map<?, ?> mReferenceObject -> {
                return castToMap(objectToCast);
            }
            case ZonedDateTime zReferenceObject -> {
                return castToDateTime(objectToCast);
            }
            case OffsetDateTime oReferenceObject -> {
                return castToDateTime(objectToCast).toOffsetDateTime();
            }
            case Instant iReferenceObject -> {
                return castToDateTime(objectToCast).toInstant();
            }
            case LocalDateTime lReferenceObject -> {
                return castToDateTime(objectToCast).toLocalDateTime();
            }
            case LocalDate dReferenceObject -> {
                return castToDate(objectToCast);
            }
            case YearMonth ymReferenceObject -> {
                return YearMonth.from(castToDate(objectToCast));
            }
            case Year yReferenceObject -> {
                return Year.from(castToDate(objectToCast));
            }
            case LocalTime tReferenceObject -> {
                return castToTime(objectToCast);
            }
            default -> {
                return null;
            }
        }
    }

    @Nonnull
    public Boolean castToBoolean(@Nullable Object value) {
        if (value == null) {
            return false;
        }

        return switch (value) {
            case Boolean bValue -> bValue;
            case Integer iValue -> iValue != 0;
            case Float fValue -> fValue != 0;
            case Double dValue -> dValue != 0;
            case Long lValue -> lValue != 0;
            case BigDecimal bdValue -> bdValue.compareTo(BigDecimal.ZERO) != 0;
            case String sValue -> {
                if (sValue.isEmpty()) {
                    yield false;
                } else if (sValue.equalsIgnoreCase("false")) {
                    yield false;
                } else if (sValue.equalsIgnoreCase("falsch")) {
                    yield false;
                } else {
                    yield true;
                }
            }
            case List<?> lValue -> !lValue.isEmpty();
            case Map<?, ?> mValue -> !mValue.isEmpty();
            default -> false;
        };
    }

    @Nonnull
    public BigDecimal castToNumber(@Nullable Object value) {
        var res = switch (value) {
            case null -> BigDecimal.ZERO;
            case Integer iValue -> BigDecimal.valueOf(iValue);
            case Float fValue -> BigDecimal.valueOf(fValue);
            case Double dValue -> BigDecimal.valueOf(dValue);
            case Long lValue -> BigDecimal.valueOf(lValue);
            case BigDecimal bdValue -> bdValue;
            case String sValue -> {
                if (isDateTimeString(sValue)) {
                    var dateTime = castToDateTime(sValue);
                    yield BigDecimal.valueOf(dateTime.toEpochSecond());
                } else if (isDateString(sValue)) {
                    yield BigDecimal.valueOf(LocalDate.parse(sValue).toEpochDay());
                } else {
                    var parsedTime = parseTime(sValue);
                    if (parsedTime != null) {
                        yield BigDecimal.valueOf(parsedTime.toSecondOfDay());
                    }

                    try {
                        yield new BigDecimal(sValue);
                    } catch (NumberFormatException e) {
                        yield BigDecimal.valueOf(sValue.length());
                    }
                }
            }
            case List<?> lValue -> BigDecimal.valueOf(lValue.size());
            case Map<?, ?> mValue -> BigDecimal.valueOf(mValue.size());
            case Instant iValue -> BigDecimal.valueOf(iValue.getEpochSecond());
            case OffsetDateTime odtValue -> BigDecimal.valueOf(odtValue.toEpochSecond());
            case LocalDate ldValue -> BigDecimal.valueOf(ldValue.toEpochDay());
            case YearMonth ymValue -> BigDecimal.valueOf(ymValue.atDay(1).toEpochDay());
            case Year yValue -> BigDecimal.valueOf(yValue.atDay(1).toEpochDay());
            case LocalTime ltValue -> BigDecimal.valueOf(ltValue.toSecondOfDay());
            case LocalDateTime ldtValue -> {
                var resolved = resolveLocalDateTime(ldtValue);
                yield resolved == null
                        ? BigDecimal.ZERO
                        : BigDecimal.valueOf(resolved.toEpochSecond());
            }
            case ZonedDateTime zdtValue -> BigDecimal.valueOf(zdtValue.toEpochSecond());
            default -> BigDecimal.ZERO;
        };

        return res.setScale(8, RoundingMode.HALF_UP);
    }

    protected int requireInteger(
            @Nullable Object value,
            @Nonnull String invalidValueMessage
    ) throws NoCodeException {
        try {
            // General no-code number casts intentionally provide permissive fallbacks.
            // Temporal components must instead reject text lengths, fractions and overflow.
            var number = switch (value) {
                case BigDecimal decimal -> decimal;
                case Number numericValue -> new BigDecimal(numericValue.toString());
                case String stringValue -> new BigDecimal(stringValue.trim());
                default -> null;
            };

            if (number != null) {
                return number.intValueExact();
            }
        } catch (ArithmeticException | NumberFormatException ignored) {
            // Convert all invalid numeric shapes into the operator's domain error below.
        }

        throw new NoCodeException(invalidValueMessage);
    }

    @Nonnull
    public String castToString(@Nullable Object value) {
        if (value == null) {
            return "";
        }

        return switch (value) {
            case String sValue -> sValue;
            case Integer iValue -> String.valueOf(iValue);
            case Float fValue -> String.valueOf(fValue);
            case Double dValue -> String.valueOf(dValue);
            case Long lValue -> String.valueOf(lValue);
            case BigDecimal bdValue -> bdValue.toString();
            case Boolean bValue -> bValue.toString();
            case List<?> lValue -> {
                try {
                    yield ObjectMapperFactory.getInstance().writeValueAsString(lValue);
                } catch (JsonProcessingException e) {
                    yield "";
                }
            }
            case Map<?, ?> mValue -> {
                try {
                    yield ObjectMapperFactory.getInstance().writeValueAsString(mValue);
                } catch (JsonProcessingException e) {
                    yield "";
                }
            }
            case Instant iValue -> IsoTimestampUtils.toOffsetString(iValue);
            case OffsetDateTime odtValue -> IsoTimestampUtils.toOffsetString(odtValue.toInstant());
            case LocalDate ldValue -> ldValue.toString();
            case YearMonth ymValue -> ymValue.toString();
            case Year yValue -> yValue.toString();
            case LocalTime ltValue -> formatLocalTime(ltValue);
            case LocalDateTime ldtValue -> {
                var resolved = resolveLocalDateTime(ldtValue);
                yield resolved == null
                        ? ""
                        : IsoTimestampUtils.toOffsetString(resolved.toInstant());
            }
            case ZonedDateTime zdtValue -> IsoTimestampUtils.toOffsetString(zdtValue.toInstant());
            default -> "";
        };
    }

    private boolean isDateTimeString(String sValue) {
        try {
            IsoTimestampUtils.parseIsoInstant(sValue);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isDateString(String value) {
        try {
            LocalDate.parse(value);
            return true;
        } catch (DateTimeParseException ignored) {
            return false;
        }
    }

    @Nullable
    private LocalTime parseTime(String value) {
        if (!value.matches("^\\d{2}:\\d{2}(?::\\d{2})?$")) {
            return null;
        }

        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    @Nonnull
    private static String formatLocalTime(@Nonnull LocalTime value) {
        // LocalTime.toString() omits zero seconds. No-code string casts use the same
        // stable HH:mm:ss representation as the HTTP and destination-payload contract.
        return value.withNano(0).format(LOCAL_TIME_SECONDS_FORMATTER);
    }

    @Nonnull
    public LocalDate castToDate(@Nullable Object value) {
        var parsedValue = tryCastToDate(value);
        return parsedValue != null
                ? parsedValue
                : LocalDate.now(ApplicationTimeZone.getZoneId());
    }

    @Nonnull
    protected LocalDate requireDate(
            @Nullable Object value,
            @Nonnull String invalidValueMessage
    ) throws NoCodeException {
        var parsedValue = tryCastToDate(value);
        if (parsedValue == null) {
            throw new NoCodeException(invalidValueMessage);
        }
        return parsedValue;
    }

    @Nonnull
    protected TemporalAccessor requireCalendarValue(
            @Nullable Object value,
            @Nonnull String invalidValueMessage
    ) throws NoCodeException {
        var parsedValue = tryCastToCalendarValue(value);
        if (parsedValue == null) {
            throw new NoCodeException(invalidValueMessage);
        }
        return parsedValue;
    }

    @Nullable
    protected TemporalAccessor tryCastToCalendarValue(@Nullable Object value) {
        return switch (value) {
            case LocalDate localDate -> localDate;
            case YearMonth yearMonth -> yearMonth;
            case Year year -> year;
            case String stringValue -> {
                try {
                    yield LocalDate.parse(stringValue);
                } catch (DateTimeParseException ignored) {
                    try {
                        yield YearMonth.parse(stringValue);
                    } catch (DateTimeParseException ignoredMonth) {
                        try {
                            yield Year.parse(stringValue);
                        } catch (DateTimeParseException ignoredYear) {
                            yield null;
                        }
                    }
                }
            }
            default -> null;
        };
    }

    @Nullable
    private LocalDate tryCastToDate(@Nullable Object value) {
        // Generic reference casts require a complete LocalDate. Partial form dates use
        // their first representable day here, then castToReferenceType restores the
        // reference value's YearMonth or Year precision. Temporal operators use
        // requireCalendarValue instead so they never invent missing calendar components.
        return switch (value) {
            case null -> null;
            case LocalDate localDate -> localDate;
            case YearMonth yearMonth -> yearMonth.atDay(1);
            case Year year -> year.atDay(1);
            case LocalDateTime localDateTime -> localDateTime.toLocalDate();
            case Instant instant -> instant.atZone(ApplicationTimeZone.getZoneId()).toLocalDate();
            case OffsetDateTime offsetDateTime ->
                    offsetDateTime.toInstant().atZone(ApplicationTimeZone.getZoneId()).toLocalDate();
            case ZonedDateTime zonedDateTime ->
                    zonedDateTime.toInstant().atZone(ApplicationTimeZone.getZoneId()).toLocalDate();
            case String stringValue -> {
                try {
                    yield LocalDate.parse(stringValue);
                } catch (DateTimeParseException ignored) {
                    try {
                        yield YearMonth.parse(stringValue).atDay(1);
                    } catch (DateTimeParseException ignoredMonth) {
                        try {
                            yield Year.parse(stringValue).atDay(1);
                        } catch (DateTimeParseException ignoredYear) {
                            try {
                                yield IsoTimestampUtils.parseIsoInstant(stringValue)
                                        .atZone(ApplicationTimeZone.getZoneId())
                                        .toLocalDate();
                            } catch (DateTimeParseException ignoredInstant) {
                                yield null;
                            }
                        }
                    }
                }
            }
            default -> null;
        };
    }

    @Nonnull
    public LocalTime castToTime(@Nullable Object value) {
        var parsedValue = tryCastToTime(value);
        return parsedValue != null
                ? parsedValue
                : LocalTime.now(ApplicationTimeZone.getZoneId()).withNano(0);
    }

    @Nonnull
    protected LocalTime requireTime(
            @Nullable Object value,
            @Nonnull String invalidValueMessage
    ) throws NoCodeException {
        var parsedValue = tryCastToTime(value);
        if (parsedValue == null) {
            throw new NoCodeException(invalidValueMessage);
        }
        return parsedValue;
    }

    @Nullable
    private LocalTime tryCastToTime(@Nullable Object value) {
        return switch (value) {
            case null -> null;
            case LocalTime localTime -> localTime.withNano(0);
            case LocalDateTime localDateTime -> localDateTime.toLocalTime().withNano(0);
            case Instant instant ->
                    instant.atZone(ApplicationTimeZone.getZoneId()).toLocalTime().withNano(0);
            case OffsetDateTime offsetDateTime ->
                    offsetDateTime.toInstant().atZone(ApplicationTimeZone.getZoneId()).toLocalTime().withNano(0);
            case ZonedDateTime zonedDateTime ->
                    zonedDateTime.toInstant().atZone(ApplicationTimeZone.getZoneId()).toLocalTime().withNano(0);
            case String stringValue -> parseTime(stringValue);
            default -> null;
        };
    }

    @Nonnull
    public ZonedDateTime castToDateTime(@Nullable Object value) {
        if (value == null) {
            return ZonedDateTime.now(ApplicationTimeZone.getZoneId());
        }

        var parsedValue = tryCastToDateTime(value);
        return parsedValue != null ? parsedValue : ZonedDateTime.now(ApplicationTimeZone.getZoneId());
    }

    @Nonnull
    protected ZonedDateTime requireDateTime(@Nullable Object value,
                                            @Nonnull String invalidValueMessage) throws NoCodeException {
        var parsedValue = tryCastToDateTime(value);
        if (parsedValue == null) {
            throw new NoCodeException(invalidValueMessage);
        }

        return parsedValue;
    }

    @Nullable
    private ZonedDateTime tryCastToDateTime(@Nullable Object value) {
        return switch (value) {
            case null -> null;
            case Instant iValue -> iValue.atZone(ApplicationTimeZone.getZoneId());
            case OffsetDateTime odtValue -> odtValue.toInstant().atZone(ApplicationTimeZone.getZoneId());
            case LocalDateTime ldtValue -> resolveLocalDateTime(ldtValue);
            case ZonedDateTime zdtValue -> zdtValue.withZoneSameInstant(ApplicationTimeZone.getZoneId());
            case String sValue -> {
                try {
                    yield IsoTimestampUtils.parseIsoInstant(sValue).atZone(ApplicationTimeZone.getZoneId());
                } catch (DateTimeParseException ignored) {
                    yield null;
                }
            }
            default -> null;
        };
    }

    @Nullable
    private ZonedDateTime resolveLocalDateTime(@Nonnull LocalDateTime value) {
        var zoneId = ApplicationTimeZone.getZoneId();
        var validOffsets = zoneId.getRules().getValidOffsets(value);
        // LocalDateTime.atZone would silently move a nonexistent DST-gap value
        // forward. No-code conversions treat that input as invalid instead.
        if (validOffsets.isEmpty()) {
            return null;
        }

        try {
            // ZoneRules lists the earlier offset first during a DST overlap.
            return value.atOffset(validOffsets.getFirst()).toInstant().atZone(zoneId);
        } catch (DateTimeException ignored) {
            return null;
        }
    }

    @Nonnull
    public Map<String, Object> castToMap(@Nullable Object value) {
        if (value == null) {
            return Map.of();
        }

        return switch (value) {
            case Map<?, ?> mValue -> (Map<String, Object>) mValue;
            case String sValue -> {
                try {
                    yield ObjectMapperFactory.getInstance().readValue(sValue, Map.class);
                } catch (JsonProcessingException e) {
                    yield Map.of();
                }
            }
            default -> {
                try {
                    var res = (Map<String, Object>) ObjectMapperFactory
                            .getInstance()
                            .convertValue(value, Map.class);
                    yield Objects.requireNonNullElse(res, Map.of());
                } catch (ClassCastException | IllegalArgumentException e) {
                    yield Map.of();
                }
            }
        };
    }

    @Nonnull
    public List<Object> castToList(@Nullable Object value) {
        if (value == null) {
            return List.of();
        }

        return switch (value) {
            case List<?> lValue -> (List<Object>) lValue;
            case String sValue -> {
                try {
                    yield ObjectMapperFactory.getInstance().readValue(sValue, List.class);
                } catch (JsonProcessingException e) {
                    yield List.of();
                }
            }
            default -> List.of();
        };
    }
}
